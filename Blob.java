package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.UUID;

/** Represents a gitlet blob object.
 *  @author Kartik Punia
 *  @author Akshay Talkad
 */
public class Blob implements Serializable {

    /** Folder that the Blobs are in. */
    static final File BLOB_FOLDER = Utils.join(Repository.GITLET_DIR, "blobs");
    /** The file that the Blob points to. */
    private byte[] file;
    /** The Sha 1 name associated with this Blob. */
    private String sha1Name;

    public Blob(String fileName) {
        file = Utils.readContents(Utils.join(Repository.CWD, fileName));

        sha1Name = Utils.sha1(UUID.randomUUID().toString());

        saveBlob();
    }

    /** Saves Blob to blobs folder in .gitlet */
    public void saveBlob() {
        Utils.writeObject(Utils.join(BLOB_FOLDER, sha1Name + ".txt"), this);
    }

    /** Reads Commit into an object given the filename. */
    public static Blob readBlob(String id) {
        return Utils.readObject(Utils.join(BLOB_FOLDER, id + ".txt"), Blob.class);
    }

    /** Returns the byte[] of the file saved in the Blob. */
    public byte[] getFile() {
        return file;
    }

    /** Returns the sha1Name of the Blob. */
    public String getName() {
        return sha1Name;
    }
}
