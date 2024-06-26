package com.example.socialmediaclient.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MessageBaseHelper extends SQLiteOpenHelper {
    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "SocialClientDB.db";

    public MessageBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + MessageDbSchema.MessageTable.NAME + "(" +
                " _id integer primary key autoincrement, " +
                MessageDbSchema.MessageTable.Cols.id + ", " +
                MessageDbSchema.MessageTable.Cols.text + ", " +
                MessageDbSchema.MessageTable.Cols.imageLocation + ", " +
                MessageDbSchema.MessageTable.Cols.datePosted + ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
}
