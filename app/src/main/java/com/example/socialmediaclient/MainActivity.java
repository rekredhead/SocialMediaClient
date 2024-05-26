package com.example.socialmediaclient;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.OAuthCredential;
import com.google.firebase.auth.OAuthProvider;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MessageAdapter.OnMessageLongClickListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_IMAGE = 1;

    private List<Message> mSelectedMessages;
    private FirebaseAuth firebaseAuth;

    private LinearLayout mSearchBar;
    private ImageButton mSearchBackButton;
    private EditText mSearchEditText;
    private Button mSearchSubmitButton;

    private LinearLayout mSubHeader;
    private TextView mSelectedMessagesCounter;
    private ImageButton mDeleteButton;
    private ImageButton mEditButton;
    private ImageButton mShareButton;
    private ImageButton mUploadButton;

    private RecyclerView mMessageBoard;
    private LinearLayoutManager mLinearLayoutManager;
    private MessageAdapter mAdapter;

    private LinearLayout mMessageCreationForm;
    private ImageView mImageSelectorImageView;
    private EditText mNewMessageEditText;
    private Button mSendNewMessageButton;

    private LinearLayout mMessageEditingForm;
    private EditText mEditMessageEditText;
    private Button mSendEditedMessageButton;

    @SuppressLint({"NotifyDataSetChanged", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSelectedMessages = new ArrayList<>();
        firebaseAuth = FirebaseAuth.getInstance();

        // Initializing Layout Components
        mSearchBar = (LinearLayout) findViewById(R.id.searchBar);
        mSearchBackButton = (ImageButton) findViewById(R.id.searchBackButton);
        mSearchEditText = (EditText) findViewById(R.id.searchEditText);
        mSearchSubmitButton = (Button) findViewById(R.id.searchSubmitButton);

        mSubHeader = (LinearLayout) findViewById(R.id.subHeader);
        mSelectedMessagesCounter = (TextView) findViewById(R.id.selectedMessagesCounter);
        mDeleteButton = (ImageButton) findViewById(R.id.deleteButton);
        mEditButton = (ImageButton) findViewById(R.id.editButton);
        mShareButton = (ImageButton) findViewById(R.id.shareButton);
        mUploadButton = (ImageButton) findViewById(R.id.uploadButton);

        mMessageBoard = (RecyclerView) findViewById(R.id.messageBoard);
        mLinearLayoutManager = new LinearLayoutManager(this);

        mMessageCreationForm = (LinearLayout) findViewById(R.id.messageCreationForm);
        mImageSelectorImageView = (ImageView) findViewById(R.id.imageSelectorImageView);
        mNewMessageEditText = (EditText) findViewById(R.id.newMessageEditText);
        mSendNewMessageButton = (Button) findViewById(R.id.newMessageSendButton);

        mMessageEditingForm = (LinearLayout) findViewById(R.id.messageEditingForm);
        mEditMessageEditText = (EditText) findViewById(R.id.editMessageEditText);
        mSendEditedMessageButton = (Button) findViewById(R.id.editMessageSendButton);

        // Enabling the message board to be scrolled to the bottom by default
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageBoard.setLayoutManager(mLinearLayoutManager);

        refreshMessageBoard();

        // Message Creation functionality
        mSendNewMessageButton.setOnClickListener(view -> {
            String textMessage = mNewMessageEditText.getText().toString().trim();
            if (!textMessage.isEmpty()) {
                Message message = new Message();
                message.setText(textMessage);

                MessageLab.get(this).addMessage(message);

                refreshMessageBoard();
                mNewMessageEditText.setText("");
            }
        });
        mImageSelectorImageView.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_IMAGE);
        });

        // Search functionality
        mSearchBackButton.setOnClickListener(view -> {
            disableSearchBar();
            refreshMessageBoard();
        });
        mSearchSubmitButton.setOnClickListener(view -> {
            String searchText = mSearchEditText.getText().toString().trim();
            if (!searchText.isEmpty()) {
                List<Message> searchedMessages = MessageLab.get(this).getMatchingMessages(searchText);

                // Making the message board render each message in the searchedMessage list
                mAdapter = new MessageAdapter(this, searchedMessages, this);
                mMessageBoard.setAdapter(mAdapter);
                mAdapter.notifyDataSetChanged();
                mSearchEditText.setText("");
            }
        });

        // Delete Functionality
        mDeleteButton.setOnClickListener(view -> {
            MessageLab.get(this).deleteMessages(mSelectedMessages);
            disableSubHeader();
            mSelectedMessages.clear();
            refreshMessageBoard();
        });

        // Edit Functionality
        mEditButton.setOnClickListener(view -> {
            disableSubHeader();

            Message selectedMessage = mSelectedMessages.get(0);
            if (selectedMessage.getText() == null) {
                Toast.makeText(this, "Only Text messages can be edited", Toast.LENGTH_SHORT).show();
                return;
            }

            // Hide the Message-Creation Form and display the Message-Editing Form
            mMessageCreationForm.setVisibility(View.INVISIBLE);
            mMessageEditingForm.setVisibility(View.VISIBLE);

            mEditMessageEditText.setText(selectedMessage.getText());

            // The actual editing is triggered from the mSendEditedMessageButton
        });
        mSendEditedMessageButton.setOnClickListener(view -> {
            Message selectedMessage = mSelectedMessages.get(0);
            String editedText = mEditMessageEditText.getText().toString().trim();
            MessageLab.get(this).editMessage(selectedMessage, editedText);

            // Revert everything back to normal
            mEditMessageEditText.setText("");
            mMessageEditingForm.setVisibility(View.GONE);
            mMessageCreationForm.setVisibility(View.VISIBLE);
            mSelectedMessages.clear();
            refreshMessageBoard();
        });

        // Share (by Email) functionality
        mShareButton.setOnClickListener(view -> {
            Message selectedMessage = mSelectedMessages.get(0);
            String subject = "Shared from SocialMediaClient";

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822"); // Type: A new email without a contactor ('To')
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);

            String body = "Date Posted: " + selectedMessage.getDatePosted();
            if (selectedMessage.getText() != null) {
                body += "\n\n" + selectedMessage.getText();
                emailIntent.putExtra(Intent.EXTRA_TEXT, body);
            } else {
                Uri imageLocation = Uri.parse(selectedMessage.getImageLocation());
                emailIntent.putExtra(Intent.EXTRA_TEXT, body);

                // Obtaining the image file and allowing the URI to be passable to external applications
                File imageFile = new File(imageLocation.getPath());
                Uri contentUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", imageFile);
                emailIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                emailIntent.setType("image/*");
                emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            if (emailIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(emailIntent);
            }

            disableSubHeader();
            mSelectedMessages.clear();
            refreshMessageBoard();
        });

        // Upload functionality
        mUploadButton.setOnClickListener(view -> {
            OAuthProvider.Builder provider = OAuthProvider.newBuilder("twitter.com");

            Task<AuthResult> pendingResultTask = firebaseAuth.getPendingAuthResult();
            if (pendingResultTask != null) {
                // There's something already here! Finish the sign-in for your user.
                pendingResultTask
                        .addOnSuccessListener(this::handleTwitterSignInResult)
                        .addOnFailureListener(e -> {
                            // Handle failure.
                            Log.e(TAG, "Error signing in with Twitter", e);
                        });
            } else {
                // There's no pending result so you need to start the sign-in flow.
                firebaseAuth.startActivityForSignInWithProvider(this, provider.build())
                        .addOnSuccessListener(this::handleTwitterSignInResult)
                        .addOnFailureListener(e -> {
                            // Handle failure.
                            Log.e(TAG, "Error signing in with Twitter", e);
                        });
            }

            disableSubHeader();
            mSelectedMessages.clear();
            refreshMessageBoard();
        });
    }

    private void handleTwitterSignInResult(AuthResult authResult) {
        OAuthCredential credential = (OAuthCredential) authResult.getCredential();
        String accessToken = credential.getAccessToken();
        String secret = credential.getSecret();

        // Use the access token and secret to post tweets
        postTweetsToTwitter(accessToken, secret);
    }

    private void postTweetsToTwitter(String accessToken, String secret) {
        new PostTweetTask(accessToken, secret).execute();
    }

    // Adding asynchronization for network operations (uploading Twitter messages) since they should not run on the main thread
    // Tweets should be uploaded in the background or on an asynchronous thread
    @SuppressLint("StaticFieldLeak")
    private class PostTweetTask extends AsyncTask<Void, Void, Void> {
        private String accessToken;
        private String secret;
        private String API_KEY = "gYS9F95d6MtnnJBnPBENCQDGs";
        private String API_SECRET = "yg9eErAJBaeFXW9JDnsVrXgv6JBBftPw8IbE50L5h97sW1Gf1t";

        public PostTweetTask(String accessToken, String secret) {
            this.accessToken = accessToken;
            this.secret = secret;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            TwitterFactory factory = new TwitterFactory();
            Twitter twitter = factory.getInstance();
            twitter.setOAuthConsumer(API_KEY, API_SECRET);
            twitter.setOAuthAccessToken(new AccessToken(accessToken, secret));

            /**
             * Due to Twitter API restrictions, sometimes the messages may not get uploaded to Twitter
             * However, the app does sign-in to the user's Twitter Account and store them in Firebase-Authentication
             * */
            for (Message msg : mSelectedMessages) {
                if (msg.getText() != null) {
                    String body = msg.getText();
                    try {
                        twitter.updateStatus(body);
                        Log.d(TAG, "Tweet posted successfully: " + body);
                    } catch (TwitterException e) {
                        Toast.makeText(MainActivity.this, "Error Posting Tweet. Please try again later", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error posting tweet", e);
                    }
                } else {
                    Uri imageUri = Uri.parse(msg.getImageLocation());
                    File imageFile = new File(imageUri.getPath());
                    try {
                        twitter.uploadMedia(imageFile);
                        Log.d(TAG, "Tweet with image posted successfully");
                    } catch (TwitterException e) {
                        Toast.makeText(MainActivity.this, "Error Posting Tweet. Please try again later", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error posting tweet with image", e);
                    }
                }
            }
            return null;
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void refreshMessageBoard() {
        List<Message> allMessages = MessageLab.get(this).getMessages();
        mAdapter = new MessageAdapter(this, allMessages, this);
        mMessageBoard.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onMessageLongClicked(View view, Message msg) {
        if (mSelectedMessages.contains(msg)) {
            view.setBackground(null);
            mSelectedMessages.remove(msg);
        } else {
            view.setBackgroundResource(R.color.teal_700);
            mSelectedMessages.add(msg);
        }

        if (!mSelectedMessages.isEmpty()) {
            disableSearchBar();
            enableSubHeader();

            // Update the selected-messages counter
            int selectedMessagesCount = mSelectedMessages.size();
            mSelectedMessagesCounter.setText(selectedMessagesCount + "");

            int visibilityType = selectedMessagesCount == 1 ? View.VISIBLE : View.GONE;
            mEditButton.setVisibility(visibilityType);
            mShareButton.setVisibility(visibilityType);
        } else {
            disableSubHeader();
            mSelectedMessages.clear();
            refreshMessageBoard();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_IMAGE) return;
        if (resultCode != RESULT_OK) return;
        if (data == null) return;

        final Uri imageLocation = data.getData();
        File storageDir = getFilesDir();
        Message imageMessage = new Message();

        String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(getContentResolver().getType(imageLocation));
        String newFileName = imageMessage.getId() + "." + ext;

        File imageFile = new File(storageDir, newFileName);

        // Creating a file in the internal storage directory
        try (
                InputStream inputStream = getContentResolver().openInputStream(imageLocation);
                FileOutputStream outputStream = new FileOutputStream(imageFile)
        ) {
            if (inputStream == null) {
                Log.e(TAG, "Selected Image's InputStream is invalid");
                return;
            }

            // Copying the data from the selected image to the output image
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            // Getting the URI of the stored file
            Uri storedImageLocation = Uri.fromFile(imageFile);
            imageMessage.setImageLocation(storedImageLocation.toString());

        } catch (IOException e) {
            Log.e(TAG, "Error occurred when storing the image: ", e);
        } finally {
            MessageLab.get(this).addMessage(imageMessage);
            refreshMessageBoard();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != R.id.searchButton) return super.onOptionsItemSelected(item);
        disableSubHeader();
        mSelectedMessages.clear();
        refreshMessageBoard();

        enableSearchBar();
        return true;
    }

    private void enableSubHeader() {
        mSubHeader.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mMessageBoard.getLayoutParams();
        layoutParams.topMargin = (int)getResources().getDimension(R.dimen.margin_top_64dp);
    }

    private void disableSubHeader() {
        mSubHeader.setVisibility(View.GONE);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mMessageBoard.getLayoutParams();
        layoutParams.topMargin = (int)getResources().getDimension(R.dimen.margin_top_0dp);
    }

    private void enableSearchBar() {
        mSearchBar.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mMessageBoard.getLayoutParams();
        layoutParams.topMargin = (int)getResources().getDimension(R.dimen.margin_top_64dp);
        mMessageBoard.setLayoutParams(layoutParams);
    }

    private void disableSearchBar() {
        mSearchBar.setVisibility(View.GONE);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mMessageBoard.getLayoutParams();
        layoutParams.topMargin = (int)getResources().getDimension(R.dimen.margin_top_0dp);
        mMessageBoard.setLayoutParams(layoutParams);
    }
}
