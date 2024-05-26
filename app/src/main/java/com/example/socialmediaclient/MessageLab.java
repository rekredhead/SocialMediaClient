package com.example.socialmediaclient;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.socialmediaclient.database.MessageBaseHelper;
import com.example.socialmediaclient.database.MessageCursorWrapper;
import com.example.socialmediaclient.database.MessageDbSchema;

import java.util.ArrayList;
import java.util.List;

public class MessageLab {
    private static MessageLab sMessageLab;
    private List<Message> mMessages;
    private Context mContext;
    private SQLiteDatabase mDatabase;

    public static MessageLab get(Context context) {
        if (sMessageLab == null) {
            sMessageLab = new MessageLab(context);
        }
        return sMessageLab;
    }

    private MessageLab(Context context) {
        mMessages = new ArrayList<>();
        mContext = context.getApplicationContext();
        mDatabase = new MessageBaseHelper(mContext).getWritableDatabase();
    }

    // Get a list of all messages
    public List<Message> getMessages() {
        List<Message> messages = new ArrayList<>();
        MessageCursorWrapper cursor = queryMessages(null, null);
        try {
            cursor.moveToFirst();
            while(!cursor.isAfterLast()) {
                messages.add(cursor.getMessage());
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }
        return messages;
    }

    // Get a list of messages that match the 'search-text' (excluding spaces, case-sensitivity and special characters)
    public List<Message> getMatchingMessages(String searchText) {
        String normalizedSearchText = normalizeString(searchText);

        List<Message> matchingMessages = new ArrayList<>();
        MessageCursorWrapper cursor = queryMessages(null, null);

        try {
            cursor.moveToFirst();
            while(!cursor.isAfterLast()) {
                Message curMessage = cursor.getMessage();
                boolean isMessageAnImage = curMessage.getText() == null;
                if (isMessageAnImage) {
                    cursor.moveToNext();
                    continue;
                }

                String normalizedMessageText = normalizeString(curMessage.getText());
                if (normalizedMessageText.contains(normalizedSearchText)) matchingMessages.add(curMessage);
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }
        return matchingMessages;
    }

    private String normalizeString(String str) {
        return str.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    public void addMessage(Message m) {
        ContentValues values = getContentValues(m);
        mDatabase.insert(MessageDbSchema.MessageTable.NAME, null, values);
    }

    public void deleteMessages(List<Message> list) {
        // Obtaining all ids from the list so the function can send a single delete query for all the ids
        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            ids.append("\"" + list.get(i).getId() + "\"");

            boolean isNotAtEndOfList = i < (list.size() - 1);
            if (isNotAtEndOfList) { ids.append(","); }
        }

        mDatabase.delete(
                MessageDbSchema.MessageTable.NAME,
                MessageDbSchema.MessageTable.Cols.id +
                        " IN (" + ids.toString() + ")",
                null
        );
    }

    public void editMessage(Message msg, String newText) {
        msg.setText(newText);
        ContentValues values = getContentValues(msg);

        mDatabase.update(
                MessageDbSchema.MessageTable.NAME,
                values,
                MessageDbSchema.MessageTable.Cols.id + " = ?",
                new String[]{String.valueOf(msg.getId())}
        );
    }

    private MessageCursorWrapper queryMessages(String whereClause, String[] whereArgs) {
        Cursor cursor = mDatabase.query(
                MessageDbSchema.MessageTable.NAME,
                null,
                whereClause,
                whereArgs,
                null,
                null,
                null
        );
        return new MessageCursorWrapper(cursor);
    }

    private static ContentValues getContentValues(Message message) {
        ContentValues values = new ContentValues();
        values.put(MessageDbSchema.MessageTable.Cols.id, message.getId().toString());
        values.put(MessageDbSchema.MessageTable.Cols.text, message.getText());
        values.put(MessageDbSchema.MessageTable.Cols.imageLocation, message.getImageLocation());
        values.put(MessageDbSchema.MessageTable.Cols.datePosted, message.getDatePosted().getTime());
        return values;
    }
}
