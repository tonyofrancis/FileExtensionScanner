package com.tonyostudio.library;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Created by tonyofrancis on 11/11/16.
 * https://github.com/tonyofrancis
 */

/**
 * The FileExtSearchService is an intent service that
 * can be used by an Android application to locate files
 * that match a certain file extension. The service is able
 * to keep a list of directory paths that it can keep track of
 * files that match the passed in file extensions.
 * The service will terminate after it scans, and a broadcast
 * intent will be sent to the system with a list of the matched files.
 * */
public class FileExtSearchService extends IntentService {

    /** Field used as a key to retrieved the passed in file extensions array from the intent that started the service. */
    public static final String EXTRA_FILE_EXTENSIONS = "file_extensions";

    /** Field used as a key to retrieved the passed in array of directory paths from the intent that started the service. */
    public static final String EXTRA_DIR_PATHS = "dir_paths";

    /** Field used as a key to indicate if the service should scan the watched directories for files that match the passed in file extensions */
    public static final String EXTRA_ACTION_SCAN = "scan_dirs";

    /** Field used as a key to retrieve the EXTRA_ADD OR EXTRA_REMOVE data from the intent that started the service */
    public static final String EXTRA_ACTION_TYPE = "action_type";

    /** Field used to retrieve the array of files that matched the passed in file extensions. Accessed in a broadcast listener
     *  that listens for the action ACTION_SEARCH_COMPLETE*/
    public static final String EXTRA_RESULTS = "results_array";

    /** Field used to indicate if the array of directory paths passed into the service should be watched and added to the watch database*/
    public static final int EXTRA_ADD = 1;

    /** Field used to indicate if the array of directory paths passed into the service should be removed from the watch database */
    public static final int EXTRA_REMOVE = 0;

    /** Intent action broadcast to the system. The Application should listen for
     * this action in a broadcast receiver to retrieve the match results */
    public static final String ACTION_SEARCH_COMPLETE = "file_search_complete";

    /** Holds an instance of FileExtSearchDatabase used by the service. This database holds
     *  all the passed in directory paths that the service needs to watch/scan */
    private FileExtSearchDatabase fileExtSearchDatabase;

    /** Convenience method used to get an intent that will start the service and scan watched
     * directories for the passed in file extensions
     *
     * @param context current context
     * @param fileExtensions file extensions to match. If null, all files and sub directories in the watched directory will be returned.
     * @param dirPaths an array of directory paths that will be watched/scanned by the service
     * @param actionType indicates whether or not the dirPath field is inserted or removed from the service watch database
     * @param scanDirs indicates if the service should start a scan
     *
     * @return a pre-configured intent that will start the file extension service
     * */
    public static Intent newIntent(@Nullable Context context, @Nullable String[] fileExtensions, @Nullable String[] dirPaths, int actionType, boolean scanDirs) {
        Intent intent = new Intent(context,FileExtSearchService.class);
        intent.putExtra(EXTRA_FILE_EXTENSIONS,fileExtensions);
        intent.putExtra(EXTRA_DIR_PATHS,dirPaths);
        intent.putExtra(EXTRA_ACTION_TYPE,actionType);
        intent.putExtra(EXTRA_ACTION_SCAN,scanDirs);

        return intent;
    }

    /** Convenience method used to get a bundle that will start the service and scan watched
     * directories for the passed in file extensions. Use this method when overriding this service.
     *
     * @param fileExtensions file extensions to match. If null, all files and sub directories in the watched directory will be returned.
     * @param dirPaths an array of directory paths that will be watched/scanned by the service
     * @param actionType indicates whether or not the dirPath field is inserted or removed from the service watch database
     * @param scanDirs indicates if the service should start a scan
     *
     * @return a pre-configured bundle that will start the file extension service
     * */
    public static Bundle newBundle(@Nullable String[] fileExtensions, @Nullable String[] dirPaths, int actionType, boolean scanDirs) {

        Bundle bundle = new Bundle();
        bundle.putStringArray(EXTRA_FILE_EXTENSIONS,fileExtensions);
        bundle.putStringArray(EXTRA_DIR_PATHS,dirPaths);
        bundle.putInt(EXTRA_ACTION_TYPE,actionType);
        bundle.putBoolean(EXTRA_ACTION_SCAN,scanDirs);

        return bundle;
    }

    /** Convenience method used to get an intent that will start the service and scan watched
     * directories
     *
     * @param context current context
     * @param fileExtensions file extensions to match. If null, all files and sub directories in the watched directory will be returned.
     *
     * @return a pre-configured intent that will start the file extension service and scan for files that
     * matches the passed in file extensions
     * */
    public static Intent newIntent(@NonNull Context context, @Nullable String[] fileExtensions) {

        Intent intent = new Intent(context,FileExtSearchService.class);
        intent.putExtra(EXTRA_ACTION_SCAN,true);
        intent.putExtra(EXTRA_FILE_EXTENSIONS,fileExtensions);

        return intent;
    }

    /** Convenience method used to get a bundle that will start the service and scan watched
     * directories. Use this method when overriding this service
     *
     * @param fileExtensions file extensions to match. If null, all files and sub directories in the watched directory will be returned.
     *
     * @return a pre-configured bundle that will start the file extension service and scan for files that
     * matches the passed in file extensions
     * */
    public static Bundle newIntent(@Nullable String[] fileExtensions) {

        Bundle bundle = new Bundle();
        bundle.putBoolean(EXTRA_ACTION_SCAN,true);
        bundle.putStringArray(EXTRA_FILE_EXTENSIONS,fileExtensions);

        return bundle;
    }

    /** Method used to retrieve a pre-configured IntentFilter that can be used
     * by a broadcast receiver in the application to listen for results from the service
     *
     * @return pre-configured IntentFilter that can be used by a BroadcastReceiver to
     * get the results generated by this service */
    public static IntentFilter newReceiverIntentFilter() {
        return new IntentFilter(ACTION_SEARCH_COMPLETE);
    }

    public FileExtSearchService() {
        super("FileExtSearchService");
    }

    /** Call this constructor if you are extending this service
     * @param name service name
     * */
    public FileExtSearchService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fileExtSearchDatabase = new FileExtSearchDatabase(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(fileExtSearchDatabase != null) {
            fileExtSearchDatabase.close();
            fileExtSearchDatabase = null;
        }
    }

    /** This method is executed on a background thread.
     *  The service processes all of its work/tasks inside this method
     *  @param intent the intent that started the service
     *  */
    @Override
    protected void onHandleIntent(Intent intent) {

        String[] dirPaths = intent.getStringArrayExtra(EXTRA_DIR_PATHS);
        int actionType = intent.getIntExtra(EXTRA_ACTION_TYPE,-1);
        boolean scanDirs = intent.getBooleanExtra(EXTRA_ACTION_SCAN,false);
        String[] fileExtensions = intent.getStringArrayExtra(EXTRA_FILE_EXTENSIONS);

        switch (actionType) {

            case EXTRA_ADD: addPathsToDatabase(dirPaths);
                break;

            case EXTRA_REMOVE: removePathsFromDatabase(dirPaths);
                break;

            default:
                break;
        }

        verifyDatabaseIntegrity();

        if(scanDirs) {

            List<String> filePaths = scanAllWatchedDirectories(fileExtensions);

            if(filePaths != null) {
                filePaths = new ArrayList<>(new LinkedHashSet<>(filePaths));
            }
            String[] matchedFiles = pathListToArray(filePaths);
            sendBroadcast(createResultIntent(matchedFiles));
            onResultsDelivered(matchedFiles);
        }
    }

    /**
     * This method will be called after the service broadcast the results of the scan.
     * Use/Override this method only when extending this service and you need the results inside of the service,
     * Otherwise listen for a broadcast intent from the app for the results of a scan.
     * Note that this method is called on the background thread.
     *
     * @param filePaths an array of file paths that matched on of the passed in file extensions
     * */
    public void onResultsDelivered(String[] filePaths) {

    }

    /**
     * Utility method used to convert a list of file paths
     * to an array of file paths.
     *
     * @param filePaths a list of file paths
     * @return an array of file paths. This method may return null
     * */
    private String[] pathListToArray(List<String> filePaths) {

        if(filePaths == null) {
            return null;
        }

        String[] matchedFiles = new String[filePaths.size()];
        filePaths.toArray(matchedFiles);
        return matchedFiles;
    }

    /** Method used to create an Intent that will be broadcast by
     * the service. Any Broadcast Receivers listening to the specified
     * action ACTION_SEARCH_COMPLETE will get the results generated by the service
     *
     * @param filePaths an array of files found that matched a specific extension in
     *                  the directories that are being watched.
     *
     * @return pre-configured intent that will be broadcast with the results.
     * Retrieve the results from the intent with the key EXTRA_RESULTS.
     * The results may be null
     * */
    private Intent createResultIntent(String[] filePaths) {

        Intent intent = new Intent(ACTION_SEARCH_COMPLETE);
        intent.putExtra(EXTRA_RESULTS,filePaths);
        return intent;
    }

    /** Utility method used to processes each watched directory into the service database
     *
     * @param dirPaths a list of file directories that will be watched by the service
     * */
    private void addPathsToDatabase(String[] dirPaths) {

        if(dirPaths == null) {
            return;
        }

        for (String dirPath : dirPaths) {

            if(dirPath != null) {
                addPathToDatabase(dirPath);
            }
        }

    }

    /** Utility method used to remove each directory path from the service database
     * @param dirPaths a list of file directories that will be removed from the service watch database
     * */
    private void removePathsFromDatabase(String[] dirPaths) {

        if(dirPaths == null) {
            return;
        }

        for (String dirPath : dirPaths) {

            if(dirPath != null) {
                removePathFromDatabase(dirPath);
            }
        }
    }

    /** Utility method used to remove a directory path from database
     * @param path Directory path that was being watched
     * */
    private void removePathFromDatabase(String path){

        if(path == null) {
            return;
        }

        if(fileExtSearchDatabase != null) {
            fileExtSearchDatabase.removePath(path);
        }
    }

    /** Utility method used to add a directory path to the database
     * @param path A directory path that will be watched
     * */
    private void addPathToDatabase(String path) {

        if(path == null) {
            return;
        }

        if(fileExistsAndIsDir(path)) {

            if(fileExtSearchDatabase != null) {
                fileExtSearchDatabase.insertPath(path);
            }
        }

    }

    /** Utility method used to verify that a file path does exist and the file is a directory
     * @param path File path
     * */
    private boolean fileExistsAndIsDir(String path) {

        if (path == null) {
            return false;
        }

        File file = new File(path);

        return file.exists() && file.isDirectory();
    }

    /** Utility method used to verify that all file paths
     *  in the database are directories and do still exist.
     *  Any file path that is not a directory or does not
     *  exist gets removed from the database
     *  */
    private void verifyDatabaseIntegrity() {
        String[] watchedDirectories = fileExtSearchDatabase.getAllPaths();


        if(watchedDirectories == null) {
            return;
        }

        for (String path : watchedDirectories) {

            if(path != null && !fileExistsAndIsDir(path)) {
                removePathFromDatabase(path);
            }
        }
    }

    /** This method gets all the watched directories from the database and scans them
     * and their sub folders for matching files with the passed in file extensions
     *
     * @param fileExtensions file extensions to match. If null, all files and sub directories in the watched directory will be returned.
     * @return A list of files that match the passed in file extensions. If no extensions
     * are passed in, this method will return all files and sub directories in the watched directory.
     * */
    private List<String> scanAllWatchedDirectories(String[] fileExtensions) {

        String[] watchedDirectories = fileExtSearchDatabase.getAllPaths();

        if(watchedDirectories == null) {
            return null;
        }

        List<String> matchedFilePaths = new ArrayList<>();

        for (String filePath : watchedDirectories) {

            if(filePath != null) {

                File file = new File(filePath);
                List<String> results = locateAndProcess(file,fileExtensions);

                if(results != null) {
                    matchedFilePaths.addAll(results);
                }
            }

        }

        return matchedFilePaths;
    }

    /**
     *  This method locates and processes all files in all sub directories located inside of
     *  the watched directory.
     *
     *  @param dir watched directory file
     *  @param fileExtensions file extensions that each file is matched against. If this parameter
     *                        is null, all files and sub-sub directory files in the directory will be returned.
     *
     *  @return a list of absolute file paths that matches one or more of the file extensions passed in. If no file
     *  extensions were passed in, this method will return all files and sub directories. If no matches were found, an empty list
     *  will be returned.
     * */
    private List<String> locateAndProcess(File dir,String[] fileExtensions) {

        List<String> matchedFilesPaths = new ArrayList<>();

        if(dir == null) {
            return matchedFilesPaths;
        }


        File[] fileMatches = getFilesWithExt(dir,fileExtensions);

        if(fileMatches != null) {
            matchedFilesPaths.addAll(filesToPathList(fileMatches));
        }


        File[] subDirs = getSubDirs(dir);
        if(subDirs != null) {

            for (File subDir : subDirs) {
                matchedFilesPaths.addAll(locateAndProcess(subDir,fileExtensions));
            }
        }

        return matchedFilesPaths;
    }

    /** This method returns a list of all sub-directories located inside of the passed in file directory.
     *
     * @param file directory file.
     * @return an array of all the sub-directories located inside of the passed in directory. If the passed
     * in file is null or not a directory, this method will return null.
     * */
    private File[] getSubDirs(File file) {

        if(file == null) {
            return null;
        }

        if(!file.isDirectory()) {
            return null;
        }

        return file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
    }

    /** Utility method that will return a list of files that matches one of the file
     *  extensions passed in.
     *
     *  @param file directory to scan
     *  @param fileExtensions file extensions that each file is matched against. If this parameter
     *                        is null, all files and sub directories in the directory will be returned.
     *
     *  @return an array of files that matches one or more of the file extensions passed in. If the fileExtensions
     *  parameter is null, all files and sub directories in the directory is returned. If the passed in file directory is null,
     *  this method will return null.
     *  */
    private File[] getFilesWithExt(File file, final String[] fileExtensions) {

        if(file == null) {
            return null;
        }

        if(fileExtensions == null) {
            return file.listFiles();
        }


        return file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String fileName) {

                for(String e : fileExtensions) {

                    if(e != null && fileName.endsWith(e)) {
                        return true;
                    }
                }

                return false;
            }
        });
    }

    /** Utility method used to get the absolute path of each file in the passed in array
     *
     * @param files an array of files
     * @return a list of the absolute paths of each file from the passed in array. This method will return
     * an empty list if the passed in files array is null.
     * */
    private List<String> filesToPathList(File[] files) {

        List<String> paths = new ArrayList<>();

        if(files == null) {
            return paths;
        }

        for (File file : files) {
            paths.add(file.getAbsolutePath());
        }

        return paths;
    }
}
