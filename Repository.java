package gitlet;

import java.io.File;
import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  @author Kartik Punia
 *  @author Akshay Talkad
 */
public class Repository {

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /** Constructor creates all the necessary directories and files for .gitlet */
    public Repository() {
        if (!GITLET_DIR.isDirectory()) {
            GITLET_DIR.mkdir();
        } else {
            System.out.println("A Gitlet version-control system already "
                    + "exists in the current directory.");
            System.exit(0);
        }

        if (!Commit.COMMIT_FOLDER.isDirectory()) {
            Commit.COMMIT_FOLDER.mkdir();
        }
        if (Commit.COMMIT_FOLDER.list().length == 0) {
            new Commit("initial commit");
        }

        if (!Blob.BLOB_FOLDER.isDirectory()) {
            Blob.BLOB_FOLDER.mkdir();
        }

        if (!Utils.join(GITLET_DIR, "staging.txt").isFile()) {
            new Staging();
        }
    }
}
