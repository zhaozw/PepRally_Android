package com.peprally.jeremy.peprally.adapters;


import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.peprally.jeremy.peprally.R;
import com.peprally.jeremy.peprally.activities.MessagingActivity;
import com.peprally.jeremy.peprally.custom.messaging.ChatMessage;
import com.peprally.jeremy.peprally.custom.messaging.Conversation;
import com.peprally.jeremy.peprally.db_models.DBUserConversation;
import com.peprally.jeremy.peprally.utils.AsyncHelpers;
import com.peprally.jeremy.peprally.utils.Helpers;
import com.peprally.jeremy.peprally.custom.UserProfileParcel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConversationCardAdapter extends RecyclerView.Adapter<ConversationCardAdapter.MessageCardHolder>{

    private Context callingContext;

    private List<DBUserConversation> conversations;

    private UserProfileParcel userProfileParcel;

    public ConversationCardAdapter(Context callingContext,
                                   List<DBUserConversation> conversations,
                                   UserProfileParcel userProfileParcel) {
        this.callingContext = callingContext;
        this.conversations = conversations;
        this.userProfileParcel = userProfileParcel;
    }

    static class MessageCardHolder extends RecyclerView.ViewHolder {
        LinearLayout clickableContainer;
        ImageView profileImage;
        TextView username;
        TextView lastMessageContent;
        TextView timeStamp;

        private MessageCardHolder(View itemView) {
            super(itemView);
            clickableContainer = (LinearLayout) itemView.findViewById(R.id.id_recycler_view_container_conversation);
            profileImage = (ImageView) itemView.findViewById(R.id.id_conversation_card_profile_photo);
            username = (TextView) itemView.findViewById(R.id.id_conversation_card_username);
            lastMessageContent = (TextView) itemView.findViewById(R.id.id_conversation_card_content);
            timeStamp = (TextView) itemView.findViewById(R.id.id_conversation_card_time_stamp);
        }
    }

    @Override
    public MessageCardHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_conversations, parent, false);
        return new MessageCardHolder(view);
    }

    @Override
    public void onBindViewHolder(MessageCardHolder MessageCardHolder, int position) {
        final DBUserConversation userConversation = conversations.get(position);
        final Conversation conversation = userConversation.getConversation();
        final String receiverUsername = userConversation.getOtherUsername(userProfileParcel.getCurrentUsername());
        final Map<String, String> usernameFacebookIDMap = conversation.getUsernameFacebookIdMap();

        Helpers.setFacebookProfileImage(callingContext,
                MessageCardHolder.profileImage,
                usernameFacebookIDMap.get(receiverUsername),
                3,
                true);
        MessageCardHolder.username.setText(receiverUsername);

        ArrayList<ChatMessage> messages = conversation.getChatMessages();
        if (messages != null && messages.size() > 0) {
            String preview = messages.get(messages.size() - 1).getMessageContent();
            if (preview.length() > 40) {
                preview = preview.substring(0, 50) + "...";
            }
            MessageCardHolder.lastMessageContent.setText(preview);
        }
        else {
            MessageCardHolder.lastMessageContent.setVisibility(View.INVISIBLE);
            MessageCardHolder.lastMessageContent.setHeight(0);
        }

        MessageCardHolder.timeStamp.setText(Helpers.getTimetampString(userConversation.getTimeStampLatest(), true));

        // onclick handlers
        MessageCardHolder.profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AsyncHelpers.launchExistingUserProfileActivity(callingContext, receiverUsername, userProfileParcel.getCurrentUsername());
            }
        });

        MessageCardHolder.clickableContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(callingContext, MessagingActivity.class);
                intent.putExtra("CONVERSATION", conversation);
                intent.putExtra("CURRENT_USERNAME", userProfileParcel.getCurrentUsername());
                intent.putExtra("CURRENT_USER_FACEBOOK_ID", userProfileParcel.getFacebookID());
                callingContext.startActivity(intent);
                ((AppCompatActivity) callingContext).overridePendingTransition(R.anim.right_in, R.anim.left_out);
            }
        });
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

}
