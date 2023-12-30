package gitlet;

import java.util.Arrays;
import java.util.List;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Kartik Punia
 *  @author Akshay Talkad
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        Staging s = null;
        if (!args[0].equals("init")) {
            if (!Arrays.asList(Repository.CWD.list()).contains(".gitlet")) {
                System.out.println("Not in an initialized Gitlet directory.");
                System.exit(0);
            }
            s = Staging.readStaging();
        }
        switch (args[0]) {
            case "init":
                new Repository();
                break;
            case "add":
                operandsChecker(1, args.length);
                s.addStaging(args[1]);
                break;
            case "commit":
                operandsChecker(1, args.length);
                s.commitStaging(args[1], null);
                break;
            case "rm":
                operandsChecker(1, args.length);
                s.removeStaging(args[1]);
                break;
            case "log":
                s.log();
                break;
            case "global-log":
                s.globalLog();
                break;
            case "find":
                operandsChecker(1, args.length);
                s.find(args[1]);
                break;
            case "status":
                s.status();
                break;
            case "checkout":
                if (args[1].equals("--") && args.length == 3) {
                    s.checkout(args[2]);
                } else if (args.length == 4) {
                    args[1] = commitChecker(args[1]);
                    if (!args[2].equals("--")) {
                        operandsChecker(1, 100);
                    }
                    s.checkout(Commit.readCommit(args[1]), args[3]);
                } else if (args.length == 2) {
                    s.checkoutBranch(args[1]);
                } else {
                    operandsChecker(1, 100);
                }
                break;
            case "branch":
                operandsChecker(1, args.length);
                s.branch(args[1]);
                break;
            case "rm-branch":
                operandsChecker(1, args.length);
                s.rmBranch(args[1]);
                break;
            case "reset":
                operandsChecker(1, args.length);
                commitChecker(args[1]);
                s.reset(args[1]);
                break;
            case "merge":
                operandsChecker(1, args.length);
                s.merge(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
    }

    /** Checks if there is the correct number of operands. */
    public static void operandsChecker(int operands, int argsLength) {
        if (argsLength != operands + 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    /** Checks if the given Commit exists in the commits folder. */
    public static String commitChecker(String commit) {
        if (commit.length() < 40) {
            commit = longUID(commit);
        }
        List<String> commitFiles = Utils.plainFilenamesIn(Commit.COMMIT_FOLDER);
        if (!commitFiles.contains(commit + ".txt")) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        return commit;
    }

    /** Converts a shortened UID into its longer version. */
    public static String longUID(String shortUID) {
        List<String> commitFiles = Utils.plainFilenamesIn(Commit.COMMIT_FOLDER);
        for (int i = 0; i < commitFiles.size(); i++) {
            if (commitFiles.get(i).substring(0, shortUID.length()).equals(shortUID)) {
                return commitFiles.get(i).substring(0, commitFiles.get(i).length() - 4);
            }
        }
        System.out.println("No commit with that id exists.");
        System.exit(0);
        return null;
    }
}
