package com.peprally.jeremy.peprally.activities;

import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedQueryList;
import com.peprally.jeremy.peprally.R;
import com.peprally.jeremy.peprally.adapters.EmptyAdapter;
import com.peprally.jeremy.peprally.adapters.NotificationCardAdapter;
import com.peprally.jeremy.peprally.db_models.DBUserNotification;
import com.peprally.jeremy.peprally.db_models.DBUserProfile;
import com.peprally.jeremy.peprally.enums.ActivityEnum;
import com.peprally.jeremy.peprally.network.DynamoDBHelper;
import com.peprally.jeremy.peprally.custom.UserProfileParcel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    /***********************************************************************************************
     *************************************** CLASS VARIABLES ***************************************
     **********************************************************************************************/
    // UI Variables
    private RecyclerView recyclerView;
    private SwipeRefreshLayout notificationsSwipeRefreshContainer;

    // AWS Variables
    private DynamoDBHelper dynamoDBHelper;

    // General Variables
    private UserProfileParcel userProfileParcel;

    /***********************************************************************************************
     *************************************** ACTIVITY METHODS **************************************
     **********************************************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        dynamoDBHelper = new DynamoDBHelper(this);

        userProfileParcel = getIntent().getParcelableExtra("USER_PROFILE_PARCEL");
        userProfileParcel.setCurrentActivity(ActivityEnum.NOTIFICATIONS);

        // setup home button on action bar
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Temporarily set recyclerView to an EmptyAdapter until we fetch real data
        recyclerView = (RecyclerView) findViewById(R.id.id_recycler_view_notifications);
        LinearLayoutManager rvLayoutManager = new LinearLayoutManager(this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(new EmptyAdapter());
        recyclerView.setLayoutManager(rvLayoutManager);

        // setup swipe refresh container
        notificationsSwipeRefreshContainer = (SwipeRefreshLayout) findViewById(R.id.id_container_swipe_refresh_notifications);
        notificationsSwipeRefreshContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshAdapter();
            }
        });

        // remove user new notification alert
        new RemoveUserNewNotificationAlertAsyncTask().execute(userProfileParcel.getCurrentUsername());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAdapter();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.left_in, R.anim.right_out);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /***********************************************************************************************
     ****************************************** UI METHODS *****************************************
     **********************************************************************************************/

    private void initializeAdapter(List<DBUserNotification> results) {
        if (results != null && results.size() > 0) {
            List<DBUserNotification> notifications = new ArrayList<>();
            for (DBUserNotification userNotification : results) {
                notifications.add(userNotification);
            }
            // Reverse notifications so they are shown in ascending order w.r.t time stamp
            Collections.sort(notifications);
            Collections.reverse(notifications);
            NotificationCardAdapter notificationCardAdapter = new NotificationCardAdapter(this, notifications, userProfileParcel);
            recyclerView.swapAdapter(notificationCardAdapter, true);
        }
        else {
            recyclerView.swapAdapter(new EmptyAdapter(), true);
        }
    }

    private void refreshAdapter() {
        notificationsSwipeRefreshContainer.post(new Runnable() {
            @Override
            public void run() {
                notificationsSwipeRefreshContainer.setRefreshing(true);
            }
        });
        new FetchUserNotificationsDBTask().execute();
    }

    /***********************************************************************************************
     ****************************************** ASYNC TASKS ****************************************
     **********************************************************************************************/
    @SuppressWarnings("unchecked")
    private class FetchUserNotificationsDBTask extends AsyncTask<Void, Void, PaginatedQueryList<DBUserNotification>> {
        @Override
        protected PaginatedQueryList<DBUserNotification> doInBackground(Void... params) {
            DBUserNotification userNotification = new DBUserNotification();
            userNotification.setUsername(userProfileParcel.getCurrentUsername());
            DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression()
                    .withHashKeyValues(userNotification)
                    .withConsistentRead(true);
            return dynamoDBHelper.getMapper().query(DBUserNotification.class, queryExpression);
        }

        @Override
        protected void onPostExecute(PaginatedQueryList<DBUserNotification> results) {
            initializeAdapter(results);

            // stop refresh loading animation
            if (notificationsSwipeRefreshContainer.isRefreshing()) {
                notificationsSwipeRefreshContainer.post(new Runnable() {
                    @Override
                    public void run() {
                        notificationsSwipeRefreshContainer.setRefreshing(false);
                    }
                });
            }
        }
    }

    private class RemoveUserNewNotificationAlertAsyncTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {
            String username = strings[0];
            DBUserProfile userProfile = dynamoDBHelper.loadDBUserProfile(username);
            if (userProfile != null) {
                userProfile.setHasNewNotification(false);
                dynamoDBHelper.saveDBObject(userProfile);
            }
            return null;
        }
    }
}
