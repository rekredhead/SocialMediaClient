package com.example.socialmediaclient.database;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.example.socialmediaclient.Message;

import java.util.Date;
import java.util.UUID;

public class MessageCursorWrapper extends CursorWrapper {
    public MessageCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public Message getMessage() {
        String id = getString(getColumnIndex(MessageDbSchema.MessageTable.Cols.id));
        String text = getString(getColumnIndex(MessageDbSchema.MessageTable.Cols.text));
        String imageLocation = getString(getColumnIndex(MessageDbSchema.MessageTable.Cols.imageLocation));
        long datePosted = getLong(getColumnIndex(MessageDbSchema.MessageTable.Cols.datePosted));

        Message message = new Message(UUID.fromString(id));
        message.setText(text);
        message.setImageLocation(imageLocation);
        message.setDatePosted(new Date(datePosted));
        return message;
    }
}
