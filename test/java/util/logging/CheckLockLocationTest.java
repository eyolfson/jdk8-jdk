/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug     6244047
 * @author Jim Gish
 * @summary throw more precise IOException when pattern specifies invalid directory
 *
 * @run  main/othervm CheckLockLocationTest
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.util.logging.FileHandler;
public class CheckLockLocationTest {

    private static final String NON_WRITABLE_DIR = "non-writable-dir";
    private static final String NOT_A_DIR = "not-a-dir";
    private static final String WRITABLE_DIR = "writable-dir";
    private static final String NON_EXISTENT_DIR = "non-existent-dir";
    private static boolean runNonWritableDirTest;

    public static void main(String... args) throws IOException {
        // we'll base all file creation attempts on the system temp directory,
        // %t and also try specifying non-existent directories and plain files
        // that should be directories, and non-writable directories,
        // to exercise all code paths of checking the lock location
        // Note that on platforms like Windows that don't support
        // setWritable() on a directory, we'll skip the non-writable
        // directory test if setWritable(false) returns false.
        //
        File writableDir = setup();
        // we now have three files/directories to work with:
        //    writableDir
        //    notAdir
        //    nonWritableDir (may not be possible on some platforms)
        //    nonExistentDir (which doesn't exist)
        runTests(writableDir);
    }

    /**
     * @param writableDir in which log and lock file are created
     * @throws SecurityException
     * @throws RuntimeException
     * @throws IOException
     */
    private static void runTests(File writableDir) throws SecurityException,
            RuntimeException, IOException {
        // Test 1: make sure we can create FileHandler in writable directory
        try {
            new FileHandler("%t/" + WRITABLE_DIR + "/log.log");
        } catch (IOException ex) {
            throw new RuntimeException("Test failed: should have been able"
                    + " to create FileHandler for " + "%t/" + WRITABLE_DIR
                    + "/log.log in writable directory.", ex);
        } finally {
            // the above test leaves files in the directory.  Get rid of the
            // files created and the directory
            delete(writableDir);
        }

        // Test 2: creating FileHandler in non-writable directory should fail
        if (runNonWritableDirTest) {
            try {
                new FileHandler("%t/" + NON_WRITABLE_DIR + "/log.log");
                throw new RuntimeException("Test failed: should not have been able"
                        + " to create FileHandler for " + "%t/" + NON_WRITABLE_DIR
                        + "/log.log in non-writable directory.");
            } catch (IOException ex) {
                // check for the right exception
                if (!(ex instanceof AccessDeniedException)) {
                    throw new RuntimeException(
                        "Test failed: Expected exception was not an "
                                + "AccessDeniedException", ex);
                }
            }
        }

        // Test 3: creating FileHandler in non-directory should fail
        try {
            new FileHandler("%t/" + NOT_A_DIR + "/log.log");
            throw new RuntimeException("Test failed: should not have been able"
                    + " to create FileHandler for " + "%t/" + NOT_A_DIR
                    + "/log.log in non-directory.");
        } catch (IOException ex) {
            // check for the right exception
            if (!(ex instanceof FileSystemException
                    && ex.getMessage().contains("Not a directory"))) {
                throw new RuntimeException(
                        "Test failed: Expected exception was not a "
                        + "FileSystemException", ex);
            }
        }

        // Test 4: make sure we can't create a FileHandler in a non-existent dir
        try {
            new FileHandler("%t/" + NON_EXISTENT_DIR + "/log.log");
            throw new RuntimeException("Test failed: should not have been able"
                    + " to create FileHandler for " + "%t/" + NON_EXISTENT_DIR
                    + "/log.log in a non-existent directory.");
        } catch (IOException ex) {
            // check for the right exception
            if (!(ex instanceof NoSuchFileException)) {
                throw new RuntimeException("Test failed: Expected exception "
                        + "was not a NoSuchFileException", ex);
            }
        }
    }

    /**
     * Setup all the files and directories needed for the tests
     *
     * @return writable directory created that needs to be deleted when done
     * @throws RuntimeException
     */
    private static File setup() throws RuntimeException {
        // First do some setup in the temporary directory (using same logic as
        // FileHandler for %t pattern)
        String tmpDir = System.getProperty("java.io.tmpdir"); // i.e. %t
        if (tmpDir == null) {
            tmpDir = System.getProperty("user.home");
        }
        File tmpOrHomeDir = new File(tmpDir);
        // Create a writable directory here (%t/writable-dir)
        File writableDir = new File(tmpOrHomeDir, WRITABLE_DIR);
        if (!createFile(writableDir, true)) {
            throw new RuntimeException("Test setup failed: unable to create"
                    + " writable working directory "
                    + writableDir.getAbsolutePath() );
        }
        // writableDirectory and its contents will be deleted after the test
        // that uses it

        // Create a plain file which we will attempt to use as a directory
        // (%t/not-a-dir)
        File notAdir = new File(tmpOrHomeDir, NOT_A_DIR);
        if (!createFile(notAdir, false)) {
            throw new RuntimeException("Test setup failed: unable to a plain"
                    + " working file " + notAdir.getAbsolutePath() );
        }
        notAdir.deleteOnExit();

        // Create a non-writable directory (%t/non-writable-dir)
        File nonWritableDir = new File(tmpOrHomeDir, NON_WRITABLE_DIR);
        if (!createFile(nonWritableDir, true)) {
            throw new RuntimeException("Test setup failed: unable to create"
                    + " a non-"
                    + "writable working directory "
                    + nonWritableDir.getAbsolutePath() );
        }
        nonWritableDir.deleteOnExit();

        // make it non-writable
        if (nonWritableDir.setWritable(false)) {
            runNonWritableDirTest = true;
        } else {
            runNonWritableDirTest = false;
            System.out.println( "Test Setup WARNING: unable to make"
                    + " working directory " + nonWritableDir.getAbsolutePath()
                    + " non-writable on platform " + System.getProperty("os.name"));

        }

        // make sure non-existent directory really doesn't exist
        File nonExistentDir = new File(tmpOrHomeDir, NON_EXISTENT_DIR);
        if (nonExistentDir.exists()) {
            nonExistentDir.delete();
        }
        return writableDir;
    }

    /**
     * @param newFile
     * @return true if file already exists or creation succeeded
     */
    private static boolean createFile(File newFile, boolean makeDirectory) {
        if (newFile.exists()) {
            return true;
        }
        if (makeDirectory) {
            return newFile.mkdir();
        } else {
            try {
                return newFile.createNewFile();
            } catch (IOException ioex) {
                ioex.printStackTrace();
                return false;
            }
        }
    }

    /*
     * Recursively delete all files starting at specified file
     */
    private static void delete(File f) throws IOException {
        if (f != null && f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
        if (!f.delete())
            throw new FileNotFoundException("Failed to delete file: " + f);
    }
}
