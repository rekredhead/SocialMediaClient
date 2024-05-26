package com.example.socialmediaclient.database;

public class MessageDbSchema {
    public static final class MessageTable {
        public static final String NAME = "messages";
        public static final class Cols {
            public static final String id = "id";
            public static final String text = "text";
            public static final String imageLocation = "imageLocation";
            public static final String datePosted = "date";
        }
    }
}
