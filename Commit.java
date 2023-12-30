package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

/** Represents a gitlet commit object.
 *  @author Kartik Punia
 *  @author Akshay Talkad
 */
public class Commit implements Serializable {

    /** Folder that the Commits are in. */
    static final File COMMIT_FOLDER = Utils.join(Repository.GITLET_DIR, "commits");
    /** The message of the Commit. */
    private String message;
    /** The timestamp of the Commit. */
    private Date timestamp;
    /** The Sha 1 name associated with this Commit. */
    private String sha1Name;
    /** All the files in the Commit (key = filename, value = sha1 name). */
    private HashMap<String, String> files;
    /** The Commit before this Commit's Sha-1 name. */
    private String prevCommit;
    /** The other parent Commit in the event of a merge. */
    private String prevCommit2;

    /** Main constructor for creating Commit object. */
    public Commit(String m, String pC, String[] fileNames, String[] blobs) {
        message = m;
        timestamp = new Date();
        sha1Name = Utils.sha1(UUID.randomUUID().toString());
        prevCommit = pC;
        prevCommit2 = null;

        files = new HashMap<>();
        for (int i = 0; i < fileNames.length; i++) {
            files.put(fileNames[i], blobs[i]);
        }

        saveCommit();
    }

    /** Constructor for creating Commit object after a merge. */
    public Commit(String m, String pC, String pC2, String[] fileNames, String[] blobs) {
        message = m;
        timestamp = new Date();
        sha1Name = Utils.sha1(UUID.randomUUID().toString());
        prevCommit = pC;
        prevCommit2 = pC2;

        files = new HashMap<>();
        for (int i = 0; i < fileNames.length; i++) {
            files.put(fileNames[i], blobs[i]);
        }

        saveCommit();
    }

    /** Constructor for creating commit0. */
    public Commit(String m) {
        message = m;
        Calendar c = Calendar.getInstance();
        c.set(1970, 0, 1, 0, 0, 0);
        timestamp = c.getTime();
        sha1Name = Utils.sha1("commit0");
        prevCommit = null;
        prevCommit2 = null;
        files = null;

        saveCommit();
    }

    /** Saves Commit to commits folder in .gitlet as a file. */
    public void saveCommit() {
        Utils.writeObject(Utils.join(COMMIT_FOLDER, sha1Name + ".txt"), this);
    }

    /** Reads Commit into an object given the filename. */
    public static Commit readCommit(String id) {
        return Utils.readObject(Utils.join(COMMIT_FOLDER, id + ".txt"), Commit.class);
    }

    /** Returns the message of the Commit. */
    public String getMessage() {
        return message;
    }

    /** Returns the timestamp of the Commit. */
    public Date getTimestamp() {
        return timestamp;
    }

    /** Returns the name of this Commit. */
    public String getName() {
        return sha1Name;
    }

    /** Returns all the files in the Commit. */
    public HashMap<String, String> getFiles() {
        if (files == null) {
            return new HashMap<>();
        }
        return files;
    }

    /** Returns the name of the Commit directly preceding this one. */
    public String getPrevCommit() {
        return prevCommit;
    }

    /** Returns the name of the other Commit directly preceding this one. */
    public String getPrevCommit2() {
        return prevCommit2;
    }
}
