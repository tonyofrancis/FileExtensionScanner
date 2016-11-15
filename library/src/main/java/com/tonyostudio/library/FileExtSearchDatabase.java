package com.tonyostudio.library;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by tonyofrancis on 11/11/16.
 * https://github.com/tonyofrancis
 */

/**
 * SQLite Database Helper used to store directory paths that need
 * to be watched and scanned by the background service FileExtSearchService.
 * Heavy database operations should always be done off of the MainThread(UI Thread)
 * to ensure performance. The FileExtSearchService class should be the only
 * class accessing the FileExtSearchDatabase to ensure data consistency.
 * */
public class FileExtSearchDatabase extends SQLiteOpenHelper {

    /** Database version*/
    public static final int VERSION = 1;

    /** Database file name*/
    public static final String NAME = "file_ext_scanner.db";

    /**
     * Class used to hold the table name and column names for the single table
     * in the database.
     * */
    public static class TABLE {

        /** Name of the single table in the database*/
        public static final String NAME = "dirPaths";

        /** Class that holds all the column names for the single table hosted in the database */
        public static class Cols {

            /** Auto generated id column for each item in the table. Each item in the table will be unique */
            public static final String ID = "_id";

            /** Column that holds a specified absolute file path */
            public static final String PATH = "dir_path";
        }
    }


    /**
     * @param context current context
     * */
    public FileExtSearchDatabase(Context context) {
        super(context, NAME, null, VERSION);
    }

    /**
     * Method used to create the database tables if the database file was newly created.
     * @param sqLiteDatabase SQLite database.*/
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE.NAME + " ( " + TABLE.Cols.ID
                + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + TABLE.Cols.PATH + " TEXT NOT NULL );");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
    }

    /** Method used to get all the absolute file paths stored in the single table
     * @return an array of all the saved paths in the table. Null may be returned
     * if the operation fails or no data was found */
    public String[] getAllPaths() {

        Cursor cursor = getReadableDatabase().query(TABLE.NAME,null,null,null,null,null,null);

        if(cursor == null) {
            return null;
        }

        String[] paths = new String[cursor.getCount()];

        cursor.moveToFirst();
        while(!cursor.isAfterLast()) {

            String path = cursorToPath(cursor);
            paths[cursor.getPosition()] = path;

            cursor.moveToNext();
        }


        if(!cursor.isClosed()) {
            cursor.close();
        }

        return paths;
    }

    /** Method used to extract a single row/data from the passed in cursor.
     * @param cursor cursor already pointed at the location to extract data
     * @return The absolute file path data. Null may be returned
     * */
    private String cursorToPath(Cursor cursor) {

        if(cursor == null) {
            return null;
        }

        return cursor.getString(cursor.getColumnIndex(TABLE.Cols.PATH));
    }

    /**
     * Method  used to insert a file path into the single table of the database
     * @param path absolute file path.
     * */
    public void insertPath(String path) {

        if(path == null) {
            return;
        }

        if(!containsPath(path)) {

            ContentValues contentValues = new ContentValues();
            contentValues.put(TABLE.Cols.PATH,path);

            getWritableDatabase().insert(TABLE.NAME,null,contentValues);
        }
    }

    /** Method used to check if a file path already exist in the single table of the database
     * @param path path to match against
     * @return method returns true if a match is found or false if not match is found
     * */
    public boolean containsPath(String path) {

        if(path == null) {
            return false;
        }

        Cursor cursor = getReadableDatabase().query(TABLE.NAME,null, TABLE.Cols.PATH + "=?",new String[]{path},null,null,null);

        int count = 0;

        if(cursor != null) {

            count = cursor.getCount();

            if(!cursor.isClosed()) {
                cursor.close();
            }
        }

        return count > 0;
    }

    /** Method used to remove a specified file path from the database
     * @param path path to match against
     * */
    public void removePath(String path) {

        if(path == null) {
            return;
        }

        getWritableDatabase().delete(TABLE.NAME, TABLE.Cols.PATH + "=?",new String[]{path});
    }
}
