package com.example.socialmediaclient;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    // This interface allows any class to implement the longclicklistener function for the messages in the Message Board
    public interface OnMessageLongClickListener {
        void onMessageLongClicked(View view, Message msg);
    }

    private List<Message> mMessages;
    private Context mContext;
    private OnMessageLongClickListener mLongClickListener;
    private SimpleDateFormat dateFormat;

    @SuppressLint("SimpleDateFormat")
    public MessageAdapter (Context context, List<Message> messages, OnMessageLongClickListener listener) {
        mContext = context;
        mMessages = messages;
        mLongClickListener = listener;
        dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new MessageViewHolder(LayoutInflater.from(mContext)
                .inflate(R.layout.item_message, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder viewHolder, int position) {
        viewHolder.bind(mMessages.get(position), mLongClickListener);
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {
        private TextView mMessageTextView;
        private ImageView mMessageImageView;
        private TextView mMsgDatePostedTextView;

        public MessageViewHolder(View v) {
            super(v);
            mMessageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            mMessageImageView = (ImageView) itemView.findViewById(R.id.messageImageView);
            mMsgDatePostedTextView = (TextView) itemView.findViewById(R.id.msgDatePostedTextView);
        }

        public void bind(Message message, OnMessageLongClickListener listener) {
            String formattedDate = dateFormat.format(message.getDatePosted());

            mMessageTextView.setText(message.getText());
            mMsgDatePostedTextView.setText(formattedDate);

            if (message.getImageLocation() != null) {
                mMessageImageView.setVisibility(View.VISIBLE);
                mMessageImageView.setImageURI(Uri.parse(message.getImageLocation()));
            } else {
                mMessageImageView.setVisibility(View.GONE);
            }

            itemView.setOnLongClickListener(view -> {
                listener.onMessageLongClicked(view, message);
                return true;
            });
        }
    }
}