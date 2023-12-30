package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.text.SimpleDateFormat;

/** Represents the current state of the gitlet staging area.
 *  @author Kartik Punia
 *  @author Akshay Talkad
 */
public class Staging implements Serializable {

    /** All the files and their corresponding blobs that are tracked in the Commit. */
    private HashMap<String, String> filesAndBlobs;
    /** All the files and their corresponding blobs that are currently in the staging area. */
    private HashMap<String, String> stagingArea;
    /** All the files that are currently staged for removal. */
    private ArrayList<String> removalStaging;
    /** The name of the last Commit. */
    private String prevCommit;
    /** All the branches and their corresponding Commits. */
    private HashMap<String, String> branches;
    /** The name of the branch that it is currently on. */
    private String currentBranch;

    /** Constructor creates the Staging file. */
    public Staging() {
        filesAndBlobs = new HashMap<>();
        stagingArea = new HashMap<>();
        removalStaging = new ArrayList<>();
        branches = new HashMap<>();
        prevCommit = Utils.sha1("commit0");
        currentBranch = "main";

        branches.put(currentBranch, prevCommit);

        saveStaging();
    }

    /** Overwrites staging.txt with the most updated staging area. */
    public void saveStaging() {
        Utils.writeObject(Utils.join(Repository.GITLET_DIR, "staging.txt"), this);
    }

    /** Reads staging.txt into an object. */
    public static Staging readStaging() {
        return Utils.readObject(Utils.join(Repository.GITLET_DIR, "staging.txt"), Staging.class);
    }

    /** Adds files to the staging area. Used for git add. */
    public void addStaging(String fileName) {
        //Checks to see if the file even exists in CWD
        List<String> cwdFiles = Utils.plainFilenamesIn(Repository.CWD);
        if (!cwdFiles.contains(fileName)) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        //Checks to see if the CWD version of file is the same as the tracked version
        if (filesAndBlobs.containsKey(fileName)
                && Arrays.equals(Utils.readContents(Utils.join(Repository.CWD, fileName)),
                        Blob.readBlob(filesAndBlobs.get(fileName)).getFile())) {
            if (stagingArea.containsKey(fileName)) {
                stagingArea.remove(fileName);
            }
        } else {
            Blob b = new Blob(fileName);
            stagingArea.put(fileName, b.getName());
        }
        if (removalStaging.contains(fileName)) {
            removalStaging.remove(fileName);
        }

        saveStaging();
    }

    /** Commits files to a Commit object. Used for git commit. */
    public void commitStaging(String message, String otherParent) {
        //Adds files from staging area
        String[] staging = stagingArea.keySet().toArray(new String[stagingArea.size()]);

        //Error checkers
        if (staging.length == 0 && removalStaging.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        if (message.isBlank()) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        for (int i = 0; i < staging.length; i++) {
            filesAndBlobs.put(staging[i], stagingArea.get(staging[i]));
        }

        //Removes files that were staged for removal
        for (int i = 0; i < removalStaging.size(); i++) {
            filesAndBlobs.remove(removalStaging.get(i));
        }

        //Turns filesAndBlobs into two arrays to pass into Commit object
        String[] addedFiles = filesAndBlobs.keySet().toArray(new String[filesAndBlobs.size()]);
        String[] addedBlobs = new String[addedFiles.length];
        for (int i = 0; i < addedFiles.length; i++) {
            addedBlobs[i] = filesAndBlobs.get(addedFiles[i]);
        }
        Commit c;
        if (otherParent != null) {
            c = new Commit(message, prevCommit, otherParent, addedFiles, addedBlobs);
        } else {
            c = new Commit(message, prevCommit, addedFiles, addedBlobs);
        }
        prevCommit = c.getName();

        //Resets staging area
        stagingArea = new HashMap<>();
        removalStaging = new ArrayList<>();

        //Updates the current branch
        branches.put(currentBranch, prevCommit);

        saveStaging();
    }

    /** Removes file from staging area and CWD. Used for git rm. */
    public void removeStaging(String arg) {
        if (!stagingArea.containsKey(arg) && !filesAndBlobs.containsKey(arg)) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }

        if (stagingArea.containsKey(arg)) {
            stagingArea.remove(arg);
        }
        if (filesAndBlobs.containsKey(arg)) {
            removalStaging.add(arg);
            Utils.restrictedDelete(Utils.join(Repository.CWD, arg));
        }

        saveStaging();
    }

    /** Prints out all Commits with the given message. Used for git find. */
    public void find(String message) {
        ArrayList<Commit> commits = new ArrayList<>();
        List<String> commitFiles = Utils.plainFilenamesIn(Commit.COMMIT_FOLDER);

        for (String commitName : commitFiles) {
            commitName = commitName.substring(0, commitName.length() - 4);
            Commit c = Commit.readCommit(commitName);
            if (c.getMessage().equals(message)) {
                commits.add(c);
            }
        }

        if (commits.isEmpty()) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }

        for (Commit history : commits) {
            System.out.println(history.getName());
        }
        System.out.println();
    }

    /** Prints out the staging area status of the CWD. Used for git status. */
    public void status() {
        System.out.println("=== Branches ===");
        String[] branchesArray = branches.keySet().toArray(new String[branches.size()]);
        Arrays.sort(branchesArray);
        for (int i = 0; i < branchesArray.length; i++) {
            if (branchesArray[i].equals(currentBranch)) {
                branchesArray[i] = "*" + branchesArray[i];
            }
            System.out.println(branchesArray[i]);
        }

        System.out.println("\n=== Staged Files ===");
        String[] staging = stagingArea.keySet().toArray(new String[stagingArea.size()]);
        Arrays.sort(staging);
        for (int i = 0; i < staging.length; i++) {
            System.out.println(staging[i]);
        }

        System.out.println("\n=== Removed Files ===");
        Collections.sort(removalStaging);
        for (int i = 0; i < removalStaging.size(); i++) {
            System.out.println(removalStaging.get(i));
        }

        System.out.println("\n=== Modifications Not Staged For Commit ===\n"
                + "\n=== Untracked Files ===\n\n");
    }

    /** Returns the state of a given file to whatever was in the head commit.
     * Used for git checkout -- [file name]. */
    public void checkout(String filename) {
        HashMap<String, String> files = Commit.readCommit(prevCommit).getFiles();
        if (files.get(filename) == null) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        Utils.writeContents(Utils.join(Repository.CWD, filename),
                Blob.readBlob(files.get(filename)).getFile());
    }

    /** Returns the state of a given file to whatever was in the given Commit.
     * Used for git checkout [commit id] -- [file name]. */
    public void checkout(Commit c, String filename) {
        HashMap<String, String> files = c.getFiles();
        if (files.get(filename) == null) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        Utils.writeContents(Utils.join(Repository.CWD, filename),
                Blob.readBlob(files.get(filename)).getFile());
    }

    /** Returns the state of the files to that of the given branch.
     * Used for git checkout [branch name]. */
    public void checkoutBranch(String branch) {
        if (!branches.containsKey(branch)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        if (currentBranch.equals(branch)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }

        String newCommit = branches.get(branch);
        currentBranch = branch;
        checkoutCommit(newCommit);
    }

    /** Returns the state of the files to that of the given branch.
     * Helper for git checkout [branch] and git reset. */
    public void checkoutCommit(String newCommit) {
        Commit prevC = Commit.readCommit(prevCommit);
        Commit newC = Commit.readCommit(newCommit);
        List<String> cwdFiles = Utils.plainFilenamesIn(Repository.CWD);
        for (int i = 0; i < cwdFiles.size(); i++) {
            if (!prevC.getFiles().containsKey(cwdFiles.get(i))
                    && newC.getFiles().containsKey(cwdFiles.get(i))) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
            }
        }

        filesAndBlobs = newC.getFiles();
        prevCommit = newC.getName();

        String[] addedFiles = newC.getFiles().keySet().toArray(new String[filesAndBlobs.size()]);
        String[] addedBlobs = new String[addedFiles.length];
        for (int i = 0; i < addedFiles.length; i++) {
            addedBlobs[i] = filesAndBlobs.get(addedFiles[i]);
        }

        for (int i = 0; i < cwdFiles.size(); i++) {
            if (prevC.getFiles().containsKey(cwdFiles.get(i))
                    && !newC.getFiles().containsKey(cwdFiles.get(i))) {
                Utils.restrictedDelete(cwdFiles.get(i));
            }
        }

        for (int i = 0; i < addedFiles.length; i++) {
            Utils.writeContents(Utils.join(Repository.CWD, addedFiles[i]),
                    Blob.readBlob(addedBlobs[i]).getFile());
        }

        stagingArea = new HashMap<>();
        removalStaging = new ArrayList<>();

        saveStaging();
    }

    /** Creates a new branch with the given name. Used for git branch. */
    public void branch(String name) {
        if (branches.containsKey(name)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        branches.put(name, prevCommit);

        saveStaging();
    }

    /** Removes a branch given its name. Used for git rm-branch. */
    public void rmBranch(String name) {
        if (!branches.containsKey(name)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (currentBranch.equals(name)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }

        branches.remove(name);

        saveStaging();
    }

    /** Checks out a given Commit and moves the branch head to this Commit.
     * Used for git reset. */
    public void reset(String newCommit) {
        branches.put(currentBranch, newCommit);
        checkoutCommit(newCommit);
    }

    /** Prints out all Commits in a commit tree starting at the head commit. Used for git log. */
    public void log() {
        ArrayList<Commit> commits = new ArrayList<>();
        commits.add(Commit.readCommit(prevCommit));
        for (int i = 0; i < 5; i++) {
            Commit c = commits.get(i);
            if (c.getName().equals(Utils.sha1("commit0"))) {
                break;
            } else {
                commits.add(Commit.readCommit(c.getPrevCommit()));
            }
        }

        for (Commit history : commits) {
            System.out.println("===");
            System.out.println("commit " + history.getName());
            Date t = history.getTimestamp();
            SimpleDateFormat format = new SimpleDateFormat("EEE MMM d kk:mm:ss yyyy Z");
            System.out.println("Date: " + format.format(t));
            System.out.println(history.getMessage());
            System.out.println();
        }
    }

    /** Prints out all Commits in the commits folder. Used for git global-log. */
    public void globalLog() {
        ArrayList<Commit> commits = new ArrayList<>();
        List<String> commitFiles = Utils.plainFilenamesIn(Commit.COMMIT_FOLDER);
        for (String commitName : commitFiles) {
            commitName = commitName.substring(0, commitName.length() - 4);
            Commit c = Commit.readCommit(commitName);
            commits.add(c);
        }

        for (Commit history : commits) {
            System.out.println("===");
            System.out.println("commit " + history.getName());
            Date t = history.getTimestamp();
            SimpleDateFormat format = new SimpleDateFormat("EEE MMM d kk:mm:ss yyyy Z");
            System.out.println("Date: " + format.format(t));
            System.out.println(history.getMessage());
            System.out.println();
        }
    }

    /** Merges a given branch into the current branch. Used for git merge. */
    public void merge(String givenBranch) {
        boolean conflicted = false;
        mergeErrorChecks(givenBranch);
        Commit givenCommit = Commit.readCommit(branches.get(givenBranch));
        Commit currentCommit = Commit.readCommit(prevCommit);
        Commit splitpoint = splitpointLocater(givenCommit, currentCommit);
        if (mergeAncestors(givenCommit, currentCommit, splitpoint, givenBranch)) {
            return;
        }
        //Merging
        List<String> splitFilesList = Arrays.asList(splitpoint.getFiles().keySet()
                .toArray(new String[splitpoint.getFiles().size()]));
        for (int i = 0; i < splitFilesList.size(); i++) {
            byte[] s = Blob.readBlob(splitpoint.getFiles().get(splitFilesList.get(i))).getFile();
            String gS = givenCommit.getFiles().get(splitFilesList.get(i));
            String cS = currentCommit.getFiles().get(splitFilesList.get(i));
            byte[] g = null;
            byte[] c = null;
            if (gS != null) {
                g = Blob.readBlob(gS).getFile();
            }
            if (cS != null) {
                c = Blob.readBlob(cS).getFile();
            }
            if (g == null && c == null) {
                continue;
            } else if (g != null && c == null) {
                if (Arrays.equals(g, s)) {
                    continue;
                } else {
                    conflict(c, g, splitFilesList.get(i)); // Conflict - Changed in g, not in c
                    conflicted = true;
                }
            } else if (g == null && c != null) {
                if (Arrays.equals(c, s)) {
                    removeStaging(splitFilesList.get(i)); // Need to remove the file
                } else {
                    conflict(c, g, splitFilesList.get(i)); // Conflict - Changed in c, not in g
                    conflicted = true;
                }
            } else if (Arrays.equals(g, s)) {
                continue;
            } else if (!Arrays.equals(g, s) && Arrays.equals(c, s)) {
                checkout(givenCommit, splitFilesList.get(i)); // Replace currFile with givenFile
                addStaging(splitFilesList.get(i)); // Stage it automatically
            } else if (Arrays.equals(g, c)) {
                continue;
            } else if (!Arrays.equals(g, c)) {
                conflict(c, g, splitFilesList.get(i)); // Conflict - Changed in different ways
                conflicted = true;
            }
        }
        String[] givenFiles = givenCommit.getFiles().keySet()
                .toArray(new String[givenCommit.getFiles().size()]);
        for (int i = 0; i < givenFiles.length; i++) {
            byte[] g = Blob.readBlob(givenCommit.getFiles().get(givenFiles[i])).getFile();
            String cS = currentCommit.getFiles().get(givenFiles[i]);
            byte[] c = null;
            if (splitFilesList.contains(givenFiles[i])) {
                continue;
            } else if (cS != null) {
                c = Blob.readBlob(cS).getFile();
            }
            if (c == null) {
                checkout(givenCommit, givenFiles[i]); // Adds givenFile
                addStaging(givenFiles[i]); // Stage it automatically
            } else if (Arrays.equals(g, c)) {
                continue;
            } else {
                conflict(c, g, givenFiles[i]); // Conflict - Changed in different ways
                conflicted = true;
            }
        }
        commitStaging("Merged " + givenBranch + " into "
                + currentBranch + ".", givenCommit.getName()); // Commits and saves staging
        if (conflicted) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** Creates a conflicted version of a file. Helper for git merge. */
    public void conflict(byte[] current, byte[] given, String filename) {
        File filePath = Utils.join(Repository.CWD, filename);

        if (current != null) {
            Utils.writeContents(filePath, current);
        }
        String firstHalf = "<<<<<<< HEAD\n" + Utils.readContentsAsString(filePath) + "=======\n";
        if (given != null) {
            Utils.writeContents(filePath, given);
        } else {
            Utils.writeContents(filePath, "");
        }
        Utils.writeContents(filePath, firstHalf
                + Utils.readContentsAsString(filePath) + ">>>>>>>\n");

        addStaging(filename);
    }

    /** Performs error checks for git merge. */
    public void mergeErrorChecks(String givenBranch) {
        if (!stagingArea.isEmpty() || !removalStaging.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (!branches.containsKey(givenBranch)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (givenBranch.equals(currentBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        Commit givenCommit = Commit.readCommit(branches.get(givenBranch));
        Commit currentCommit = Commit.readCommit(prevCommit);
        List<String> cwdFiles = Utils.plainFilenamesIn(Repository.CWD);
        for (int i = 0; i < cwdFiles.size(); i++) {
            if (!currentCommit.getFiles().containsKey(cwdFiles.get(i))
                    && givenCommit.getFiles().containsKey(cwdFiles.get(i))) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                System.exit(0);
            }
        }
    }

    /** Locates the split point between two Commits. Helper for git merge. */
    public Commit splitpointLocater(Commit givenCommit, Commit currentCommit) {
        ArrayList<String> givenComList = new ArrayList<>();
        ArrayList<String> currComList = new ArrayList<>();

        String name = givenCommit.getName();
        while (!name.equals(Utils.sha1("commit0"))) {
            givenComList.add(name);
            String name2 = Commit.readCommit(name).getPrevCommit2();
            if (name2 != null) {
                givenComList.add(splitpointLocater(givenCommit,
                        Commit.readCommit(name2)).getName());
            }
            name = Commit.readCommit(name).getPrevCommit();
        }
        givenComList.add(name);

        name = currentCommit.getName();
        while (!name.equals(Utils.sha1("commit0"))) {
            currComList.add(name);
            String name2 = Commit.readCommit(name).getPrevCommit2();
            if (name2 != null) {
                currComList.add(splitpointLocater(givenCommit, Commit.readCommit(name2)).getName());
            }
            name = Commit.readCommit(name).getPrevCommit();
        }
        currComList.add(name);

        ArrayList<String> sharedComList = new ArrayList<>();
        for (int i = 0; i < givenComList.size(); i++) {
            if (currComList.contains(givenComList.get(i))) {
                sharedComList.add(givenComList.get(i));
            }
        }
        Commit newestC = Commit.readCommit(sharedComList.get(0));
        Date newestD = newestC.getTimestamp();
        for (int i = 1; i < sharedComList.size(); i++) {
            Date next = Commit.readCommit(sharedComList.get(i)).getTimestamp();
            if (newestD.before(next)) {
                newestD = next;
                newestC = Commit.readCommit(sharedComList.get(i));
            }
        }

        return newestC;
    }

    /** Merges two Commits when one is an ancestor of the other. Helper for git merge. */
    public boolean mergeAncestors(Commit gCommit, Commit cCommit, Commit split, String gBranch) {
        if (gCommit.getName().equals(split.getName())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return true;
        } else if (cCommit.getName().equals(split.getName())) {
            checkoutBranch(gBranch);
            System.out.println("Current branch fast-forwarded.");
            return true;
        }
        return false;
    }
}
