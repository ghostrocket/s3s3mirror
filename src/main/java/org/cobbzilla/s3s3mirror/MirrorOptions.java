package org.cobbzilla.s3s3mirror;

import com.amazonaws.auth.AWSCredentials;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.DateTime;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import java.util.Date;

public class MirrorOptions implements AWSCredentials {

    public static final String S3_PROTOCOL_PREFIX = "s3://";

    public static final String AWS_ACCESS_KEY = "AWS_ACCESS_KEY_ID";
    public static final String AWS_SECRET_KEY = "AWS_SECRET_ACCESS_KEY";
    @Getter @Setter private String aWSAccessKeyId = System.getenv().get(AWS_ACCESS_KEY);
    @Getter @Setter private String aWSSecretKey = System.getenv().get(AWS_SECRET_KEY);

    public boolean hasAwsKeys() { return aWSAccessKeyId != null && aWSSecretKey != null; }

    public static final String USAGE_DRY_RUN = "Do not actually do anything, but show what would be done";
    public static final String OPT_DRY_RUN = "-n";
    public static final String LONGOPT_DRY_RUN = "--dry-run";
    @Option(name=OPT_DRY_RUN, aliases=LONGOPT_DRY_RUN, usage=USAGE_DRY_RUN)
    @Getter @Setter private boolean dryRun = false;

    public static final String USAGE_VERBOSE = "Verbose output";
    public static final String OPT_VERBOSE = "-v";
    public static final String LONGOPT_VERBOSE = "--verbose";
    @Option(name=OPT_VERBOSE, aliases=LONGOPT_VERBOSE, usage=USAGE_VERBOSE)
    @Getter @Setter private boolean verbose = false;

    public static final String USAGE_PREFIX = "Only copy objects whose keys start with this prefix";
    public static final String OPT_PREFIX = "-p";
    public static final String LONGOPT_PREFIX = "--prefix";
    @Option(name=OPT_PREFIX, aliases=LONGOPT_PREFIX, usage=USAGE_PREFIX)
    @Getter @Setter private String prefix = null;

    public boolean hasPrefix () { return prefix != null && prefix.length() > 0; }
    public int getPrefixLength () { return prefix == null ? 0 : prefix.length(); }

    public static final String USAGE_DEST_PREFIX = "Destination prefix (replacing the one specified in --prefix, if any)";
    public static final String OPT_DEST_PREFIX= "-d";
    public static final String LONGOPT_DEST_PREFIX = "--dest-prefix";
    @Option(name=OPT_DEST_PREFIX, aliases=LONGOPT_DEST_PREFIX, usage= USAGE_DEST_PREFIX)
    @Getter @Setter private String destPrefix = null;

    public boolean hasDestPrefix() { return destPrefix != null && destPrefix.length() > 0; }
    public int getDestPrefixLength () { return destPrefix == null ? 0 : destPrefix.length(); }

    public static final String USAGE_MAX_CONNECTIONS = "Maximum number of connections to S3 (default 100)";
    public static final String OPT_MAX_CONNECTIONS = "-m";
    public static final String LONGOPT_MAX_CONNECTIONS = "--max-connections";
    @Option(name=OPT_MAX_CONNECTIONS, aliases=LONGOPT_MAX_CONNECTIONS, usage=USAGE_MAX_CONNECTIONS)
    @Getter @Setter private int maxConnections = 100;

    public static final String USAGE_MAX_THREADS = "Maximum number of threads (default 100)";
    public static final String OPT_MAX_THREADS = "-t";
    public static final String LONGOPT_MAX_THREADS = "--max-threads";
    @Option(name=OPT_MAX_THREADS, aliases=LONGOPT_MAX_THREADS, usage=USAGE_MAX_THREADS)
    @Getter @Setter private int maxThreads = 100;

    public static final String USAGE_MAX_RETRIES = "Maximum number of retries for S3 requests (default 5)";
    public static final String OPT_MAX_RETRIES = "-r";
    public static final String LONGOPT_MAX_RETRIES = "--max-retries";
    @Option(name=OPT_MAX_RETRIES, aliases=LONGOPT_MAX_RETRIES, usage=USAGE_MAX_RETRIES)
    @Getter @Setter private int maxRetries = 5;

    public static final String USAGE_CTIME = "Only copy objects whose Last-Modified date is younger than this many days. " +
            "For other time units, use these suffixes: y (years), M (months), d (days), w (weeks), h (hours), m (minutes), s (seconds)";
    public static final String OPT_CTIME = "-c";
    public static final String LONGOPT_CTIME = "--ctime";
    @Option(name=OPT_CTIME, aliases=LONGOPT_CTIME, usage=USAGE_CTIME)
    @Getter @Setter private String ctime = null;
    public boolean hasCtime() { return ctime != null; }

    private long initMaxAge() {

        DateTime dateTime = new DateTime(nowTime);

        // all digits -- assume "days"
        if (ctime.matches("^[0-9]+$")) return dateTime.minusDays(Integer.parseInt(ctime)).getMillis();

        // ensure there is at leaste one digit, and exactly one character suffix, and the char is a legal option
        if (!ctime.matches("^[0-9]+[yMwdhms]$")) throw new IllegalArgumentException("Invalid option for ctime: "+ctime);

        if (ctime.endsWith("y")) return dateTime.minusYears(getCtimeNumber(ctime)).getMillis();
        if (ctime.endsWith("M")) return dateTime.minusMonths(getCtimeNumber(ctime)).getMillis();
        if (ctime.endsWith("w")) return dateTime.minusWeeks(getCtimeNumber(ctime)).getMillis();
        if (ctime.endsWith("d")) return dateTime.minusDays(getCtimeNumber(ctime)).getMillis();
        if (ctime.endsWith("h")) return dateTime.minusHours(getCtimeNumber(ctime)).getMillis();
        if (ctime.endsWith("m")) return dateTime.minusMinutes(getCtimeNumber(ctime)).getMillis();
        if (ctime.endsWith("s")) return dateTime.minusSeconds(getCtimeNumber(ctime)).getMillis();
        throw new IllegalArgumentException("Invalid option for ctime: "+ctime);
    }

    private int getCtimeNumber(String ctime) {
        return Integer.parseInt(ctime.substring(0, ctime.length() - 1));
    }

    @Getter private long nowTime = System.currentTimeMillis();
    @Getter private long maxAge;
    @Getter private String maxAgeDate;

    public static final String USAGE_DELETE_REMOVED = "Delete objects from the destination bucket if they do not exist in the source bucket";
    public static final String OPT_DELETE_REMOVED = "-X";
    public static final String LONGOPT_DELETE_REMOVED = "--delete-removed";
    @Option(name=OPT_DELETE_REMOVED, aliases=LONGOPT_DELETE_REMOVED, usage=USAGE_DELETE_REMOVED)
    @Getter @Setter private boolean deleteRemoved = false;

    @Argument(index=0, required=true, usage="source bucket[/source/prefix]") @Getter @Setter private String source;
    @Argument(index=1, required=true, usage="destination bucket[/dest/prefix]") @Getter @Setter private String destination;

    @Getter private String sourceBucket;
    @Getter private String destinationBucket;

    public void initDerivedFields() {

        if (hasCtime()) {
            this.maxAge = initMaxAge();
            this.maxAgeDate = new Date(maxAge).toString();
        }

        String scrubbed;
        int slashPos;

        scrubbed = scrubS3ProtocolPrefix(source);
        slashPos = scrubbed.indexOf('/');
        if (slashPos == -1) {
            sourceBucket = scrubbed;
        } else {
            sourceBucket = scrubbed.substring(0, slashPos);
            if (hasPrefix()) throw new IllegalArgumentException("Cannot use a "+OPT_PREFIX+"/"+LONGOPT_PREFIX+" argument and source path that includes a prefix at the same time");
            prefix = scrubbed.substring(slashPos+1);
        }

        scrubbed = scrubS3ProtocolPrefix(destination);
        slashPos = scrubbed.indexOf('/');
        if (slashPos == -1) {
            destinationBucket = scrubbed;
        } else {
            destinationBucket = scrubbed.substring(0, slashPos);
            if (hasDestPrefix()) throw new IllegalArgumentException("Cannot use a "+OPT_DEST_PREFIX+"/"+LONGOPT_DEST_PREFIX+" argument and destination path that includes a dest-prefix at the same time");
            destPrefix = scrubbed.substring(slashPos+1);
        }
    }

    protected String scrubS3ProtocolPrefix(String bucket) {
        bucket = bucket.trim();
        if (bucket.startsWith(S3_PROTOCOL_PREFIX)) {
            bucket = bucket.substring(S3_PROTOCOL_PREFIX.length());
        }
        return bucket;
    }
}
