package com.sometimestwo.jumble;

import android.annotation.SuppressLint;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.arch.paging.PagedList;
import android.arch.paging.PagedListAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;
import com.makeramen.roundedimageview.RoundedImageView;
import com.sometimestwo.jumble.EventListeners.HomeEventListener;
import com.sometimestwo.jumble.EventListeners.OnRedditTaskListener;
import com.sometimestwo.jumble.EventListeners.OnTaskCompletedListener;
import com.sometimestwo.jumble.EventListeners.OnVRedditTaskCompletedListener;
import com.sometimestwo.jumble.Model.ExpandableMenuModel;
import com.sometimestwo.jumble.Model.Explore;
import com.sometimestwo.jumble.Model.GfyItem;
import com.sometimestwo.jumble.Model.GfycatWrapper;
import com.sometimestwo.jumble.Model.SubmissionObj;
import com.sometimestwo.jumble.Utils.Constants;
import com.sometimestwo.jumble.Utils.Utils;

import net.dean.jraw.models.SubredditSort;
import net.dean.jraw.models.TimePeriod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.app.Activity.RESULT_OK;

public class FragmentHome extends Fragment {
    public final static String TAG = Constants.TAG_FRAG_HOME;

    private int mScreenWidth;
    private int mScreenHeight;
    private RequestManager GlideApp = App.getGlideApp();
    private SwipeRefreshLayout mRefreshLayout;
    private MultiClickRecyclerView mRecyclerHome;
    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationViewLeft;
    private NavigationView mNavigationViewRight;
    private ActionBarDrawerToggle mDrawerToggle;
    private String mCurrSubreddit;
    private String mCurrExploreTitle;
    private boolean isImageViewPressed = false;
    private int mActivePointerId = -1;
    private GestureDetector mGestureDetector;
    private ProgressBar mPreviewerProgressBar;
    private ProgressBar mProgressbarMain;
    private boolean mInvalidateDataSource = false;
    private boolean is404 = false;

    // User's preferences. Initialized just in case <:^)
    SharedPreferences prefs_settings;
    private boolean mHideNSFW = true;
    private boolean mAllowImagePreview = false;
    private boolean mAllowClickClose = true;
    private boolean mDisplayDomainIcon = false;
    private boolean mHideNSFWThumbs = false;
    private boolean mDisplayFiletypeIcons = false;
    private boolean mDisplayNSFWIcon = false;
    private Constants.HoverPreviewSize mPreviewSize;

    // hover preview small
    private RelativeLayout mHoverPreviewContainerSmall;
    private TextView mHoverPreviewTitleSmall;
    private ImageView mHoverImagePreviewSmall;

    // hover view large
    private RelativeLayout mHoverPreviewContainerLarge;
    private FrameLayout mHoverPreviewMediaContainerLarge;
    private TextView mHoverPreviewTitleLarge;
    private TextView mHoverPreviewSubredditLarge;
    private ImageView mHoverImagePreviewLarge;
    // private GfycatPlayer mHoverPreviewGfycatLarge;

    /* Left navigation view */
    private LinearLayout mNavViewHeader;
    private TextView mNavViewHeaderTitle;
    private ImageView mNavViewDropDown;
    ExpandableListAdapter expandableListAdapter;
    ExpandableListView expandableListView;
    List<ExpandableMenuModel> headerList = new ArrayList<>();
    HashMap<ExpandableMenuModel, List<ExpandableMenuModel>> childList = new HashMap<>();
    private TextView mButtonLogout;

    /* Right navigation view*/
    private RecyclerView mRightNavRecycleView;
    private ProgressBar mRightNavProgressBar;
    // maps an explore category to a background image uri
    private Map<String, Explore> mExploreCatagoriesMap;
    // holds a list of the "Explore" catagories
    private List<String> mExploreCatagoriesList;


    //exo player stuff
    private BandwidthMeter bandwidthMeter;
    private DefaultTrackSelector trackSelector;
    // Exoplayer is used to play streaming(as opposed to downloaded) .mp4 files.
    // .gifv links will be converted to .mp4 links and played with exoplayer.
    private SimpleExoPlayer player;
    private ProgressBar exoplayerProgressbar;
    private DataSource.Factory mediaDataSourceFactory;
    private int currentWindow;
    private long playbackPosition;
    private Timeline.Window window;
    private PlayerView mExoplayerLarge;

    // Videoview is used to play downloaded(as opposed to streaming) .mp4 files.
    // VReddit links will be downloaded and played using VideoView.
    private VideoView mPreviewerVideoViewLarge;
    // event listeners
    private HomeEventListener mHomeEventListener;

    // Async tasks that may need cancelling
    private AsyncTask<Void, Void, Void> FetchUserSubscriptionsTask;
    private AsyncTask<Void, Void, Void> SubscribeSubredditTask;
    private AsyncTask<Void, Void, Boolean> UnsubscribeSubredditTask;

    public static FragmentHome newInstance() {
        return new FragmentHome();
    }


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        prefs_settings = getContext().getSharedPreferences(Constants.KEY_SHARED_PREFS, Context.MODE_PRIVATE);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Ensure Reddit Client is authenticated before proceeding
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        /* Set screen dimensions for resizing dialogs */
        mScreenWidth = getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getResources().getDisplayMetrics().heightPixels;

        /* Initialize any preference/settings variables */
        try {
            validatePreferences();
        } catch (Exception e) {
            e.printStackTrace();
        }

        unpackArgs();

        /*Drawer layout config */
        setupDrawerLayout(v);

        /* Navigation menu on left*/
        setupLeftNavView(v);

        /* Navigation menu on right*/
        setupRightNavView(v);

        /* Toolbar setup*/
        mToolbar = (Toolbar) v.findViewById(R.id.toolbar_main);
        ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);


        /* Refresh layout setup*/
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.home_refresh_layout);
        mRefreshLayout.setDistanceToTriggerSync(Constants.REFRESH_PULL_TOLERANCE);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                FragmentHome.this.refresh(true);
            }
        });

        /* Recycler view setup*/
        mRecyclerHome = (MultiClickRecyclerView) v.findViewById(R.id.recycler_submissions);
        mRecyclerHome.setLayoutManager(new GridLayoutManager(getContext(), 3));
        mRecyclerHome.setHasFixedSize(true);

        /* RecyclerView adapter stuff */
        final SubredditContentRecyclerAdapter adapter = new SubredditContentRecyclerAdapter(getContext());

        /* Viewmodel fetching and data updating */
        if (mInvalidateDataSource) {
            // This is true when we are refreshing a page whether it be from a swipe refresh,
            // a new user log in, or user log out
            invalidateData();
        }
        SubmissionsViewModel submissionsViewModel = ViewModelProviders.of(this).get(SubmissionsViewModel.class);
        submissionsViewModel.postsPagedList.observe(getActivity(), new Observer<PagedList<SubmissionObj>>() {
            @Override
            public void onChanged(@Nullable PagedList<SubmissionObj> items) {
                // submitting changes to adapter, if any
                adapter.submitList(items);
            }
        });

        if (isAdded()) {
            mRecyclerHome.setAdapter(adapter);
        }

        /* for detecting click types when needed */
        mGestureDetector = new GestureDetector(getContext(), new SingleTapConfirm());

        mProgressbarMain = (ProgressBar) v.findViewById(R.id.progress_bar_home);
        /* Progress bar for loading images/gifs/videos*/
        mPreviewerProgressBar = (ProgressBar) v.findViewById(R.id.hover_view_large_image_media_progress);

        /* hover view small*/
        /*mHoverPreviewContainerSmall = (RelativeLayout) v.findViewById(R.id.hover_view_container_small);
        mHoverPreviewTitleSmall = (TextView) v.findViewById(R.id.hover_view_title_small);
        mHoverImagePreviewSmall = (ImageView) v.findViewById(R.id.hover_imageview_small);*/

        /* hover view large*/
        mHoverPreviewContainerLarge = (RelativeLayout) v.findViewById(R.id.hover_view_container_large);
        mHoverPreviewMediaContainerLarge = (FrameLayout) v.findViewById(R.id.hover_view_large_image_container);
        //mHoverPreviewTitleLarge = (TextView) v.findViewById(R.id.hover_view_title_large);
        //mHoverPreviewSubredditLarge = (TextView) v.findViewById(R.id.hover_view_textview_subreddit);
        mHoverImagePreviewLarge = (ImageView) v.findViewById(R.id.large_previewer_imageview);

        /* Exo player */
        //mExoplayerContainerLarge = (FrameLayout) v.findViewById(R.id.container_exoplayer_large);
        mExoplayerLarge = (PlayerView) v.findViewById(R.id.large_previewer_exoplayer);
        mPreviewerVideoViewLarge = (VideoView) v.findViewById(R.id.large_previewer_video_view);

        /* Rightnav recycler view */
        mRightNavRecycleView = (RecyclerView) v.findViewById(R.id.navview_right_recycler);

        bandwidthMeter = new DefaultBandwidthMeter();
        mediaDataSourceFactory = new DefaultDataSourceFactory(getContext(), Util.getUserAgent(getContext(), "Jumble"), (TransferListener<? super DataSource>) bandwidthMeter);
        window = new Timeline.Window();
        //ivHideControllerButton = findViewById(R.id.exo_controller);
        // progressBar = findViewById(R.id.progress_bar);

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mHomeEventListener = (HomeEventListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement listener inferfaces!");
        }
    }

    @Override
    public void onAttachFragment(Fragment childFragment) {
        super.onAttachFragment(childFragment);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Need to make sure user is authenticated
        if (!App.getAccountHelper().isAuthenticated()) {
            mHomeEventListener.startOver();
        } else {
            try {
                // Check if user settings have been altered.
                // i.e. User went to settings, opted in to NSFW posts then navigated back.
                validatePreferences();
                setupToolbar();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        releaseExoPlayer();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mToolbar.setAlpha(0);
        cancelRunning();

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseExoPlayer();
    }

    @Override
    public void onDestroyOptionsMenu() {
        super.onDestroyOptionsMenu();
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Sub/Unsub menu option only if we're not in guest mode and viewing a subreddit exists
        if (!Utils.isUserlessSafe()
                && mCurrSubreddit != null
                && !is404
                && !mCurrSubreddit.contains("+") // don't allow multi subreddit subscribe
                && !Constants.REQUEST_SAVED.equalsIgnoreCase(mCurrSubreddit)) {
            String currUsername = App.getSharedPrefs().getString(Constants.MOST_RECENT_USER, null);
            // User is subbed to this subreddit, display "Unsubscribe" option
            if (Utils.getSubscriptionsFromSharedPrefs(currUsername).contains(mCurrSubreddit)) {
                menu.findItem(R.id.menu_submissions_overflow_sub).setVisible(false);
                menu.findItem(R.id.menu_submissions_overflow_unsub).setVisible(true);
                String unsubString = getString(R.string.unsub_from, mCurrSubreddit);
                menu.findItem(R.id.menu_submissions_overflow_unsub).setTitle(unsubString);
            }
            // User is not subscribed to this subreddit, display "Subscribe" option
            else {
                menu.findItem(R.id.menu_submissions_overflow_unsub).setVisible(false);
                menu.findItem(R.id.menu_submissions_overflow_sub).setVisible(true);
                String subString = getString(R.string.sub_to, mCurrSubreddit);
                menu.findItem(R.id.menu_submissions_overflow_sub).setTitle(subString);
            }
        } else {
            menu.findItem(R.id.menu_submissions_overflow_sub).setVisible(false);
            menu.findItem(R.id.menu_submissions_overflow_unsub).setVisible(false);
        }

        // Don't display explore or sortby buttons if we're viewing Saved Submissions
        if (Constants.REQUEST_SAVED.equalsIgnoreCase(mCurrSubreddit)) {
            menu.findItem(R.id.menu_submissions_sortby).setVisible(false);
            menu.findItem(R.id.menu_explore).setVisible(false);
        } else {
            menu.findItem(R.id.menu_submissions_sortby).setVisible(true);
            menu.findItem(R.id.menu_explore).setVisible(true);
        }

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String currUser = App.getSharedPrefs().getString(Constants.MOST_RECENT_USER, null);

        switch (item.getItemId()) {
            case android.R.id.home:
                mHomeEventListener.menuGoBack();
                return true;
            case R.id.menu_explore:
                mDrawerLayout.openDrawer(GravityCompat.END);
                return true;
            case R.id.menu_submissions_overflow_refresh:
                refresh(true);
                return true;
            case R.id.menu_submissions_overflow_sub:
                SubscribeSubredditTask =
                        new Utils.SubscribeSubredditTask(mCurrSubreddit, new OnRedditTaskListener() {
                            @Override
                            public void onSuccess() {
                                String currUser = App.getSharedPrefs().getString(Constants.MOST_RECENT_USER, null);
                                Utils.addToLocalUserSubscriptions(currUser, mCurrSubreddit);
                                Toast.makeText(FragmentHome.this.getContext(),
                                        "Subscribed",
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }

                            @Override
                            public void onFailure(String exceptionMessage) {
                                Toast.makeText(FragmentHome.this.getContext(),
                                        getResources().getString(R.string.toast_subscribe_failed),
                                        Toast.LENGTH_LONG)
                                        .show();
                            }
                        }).execute();
                return true;
            case R.id.menu_submissions_overflow_unsub:
                UnsubscribeSubredditTask =
                        new Utils.UnsubscribeSubredditTask(mCurrSubreddit, new OnRedditTaskListener() {
                            @Override
                            public void onSuccess() {
                                Utils.removeFromLocalUserSubscriptions(currUser, mCurrSubreddit);
                                Toast.makeText(FragmentHome.this.getContext(),
                                        "Unsubscribed",
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }

                            @Override
                            public void onFailure(String exceptionMessage) {
                                Toast.makeText(FragmentHome.this.getContext(),
                                        getResources().getString(R.string.toast_unsubscribe_failed),
                                        Toast.LENGTH_LONG)
                                        .show();
                            }
                        }).execute();
                return true;
            /* Sort by*/
            case R.id.menu_submissions_sortby_BEST:
                App.getJumbleInfoObj().setmSortBy(SubredditSort.BEST);
                App.getJumbleInfoObj().setmTimePeriod(null);
                refresh(true);
                return true;
            case R.id.menu_submissions_sortby_HOT:
                App.getJumbleInfoObj().setmSortBy(SubredditSort.HOT);
                App.getJumbleInfoObj().setmTimePeriod(null);
                refresh(true);
                return true;
            case R.id.menu_submissions_sortby_NEW:
                App.getJumbleInfoObj().setmSortBy(SubredditSort.NEW);
                App.getJumbleInfoObj().setmTimePeriod(null);
                refresh(true);
                return true;
            case R.id.menu_submissions_sortby_RISING:
                App.getJumbleInfoObj().setmSortBy(SubredditSort.RISING);
                App.getJumbleInfoObj().setmTimePeriod(null);
                refresh(true);
                return true;
            case R.id.menu_submissions_sortby_TOP_hour:
                App.getJumbleInfoObj().setmSortBy(SubredditSort.TOP);
                App.getJumbleInfoObj().setmTimePeriod(TimePeriod.HOUR);
                refresh(true);
                return true;

            case R.id.menu_submissions_sortby_TOP_today:
                App.getJumbleInfoObj().setmSortBy(SubredditSort.TOP);
                App.getJumbleInfoObj().setmTimePeriod(TimePeriod.DAY);
                refresh(true);
                return true;

            case R.id.menu_submissions_sortby_TOP_week:
                App.getJumbleInfoObj().setmSortBy(SubredditSort.TOP);
                App.getJumbleInfoObj().setmTimePeriod(TimePeriod.WEEK);
                refresh(true);
                return true;

            case R.id.menu_submissions_sortby_TOP_month:
                App.getJumbleInfoObj().setmSortBy(SubredditSort.TOP);
                App.getJumbleInfoObj().setmTimePeriod(TimePeriod.MONTH);
                refresh(true);
                return true;

            case R.id.menu_submissions_sortby_TOP_year:
                App.getJumbleInfoObj().setmSortBy(SubredditSort.TOP);
                App.getJumbleInfoObj().setmTimePeriod(TimePeriod.YEAR);
                refresh(true);
                return true;

            case R.id.menu_submissions_sortby_TOP_alltime:
                App.getJumbleInfoObj().setmSortBy(SubredditSort.TOP);
                App.getJumbleInfoObj().setmTimePeriod(TimePeriod.ALL);
                refresh(true);
                return true;
            case R.id.menu_submissions_sortby_CONTROVERSIAL_hour:
                App.getJumbleInfoObj().setmSortBy(SubredditSort.CONTROVERSIAL);
                App.getJumbleInfoObj().setmTimePeriod(TimePeriod.HOUR);
                refresh(true);
                return true;
            case R.id.menu_submissions_sortby_CONTROVERSIAL_today:
                App.getJumbleInfoObj().setmSortBy(SubredditSort.CONTROVERSIAL);
                App.getJumbleInfoObj().setmTimePeriod(TimePeriod.DAY);
                refresh(true);
                return true;
            case R.id.menu_submissions_sortby_CONTROVERSIAL_week:
                App.getJumbleInfoObj().setmSortBy(SubredditSort.CONTROVERSIAL);
                App.getJumbleInfoObj().setmTimePeriod(TimePeriod.WEEK);
                refresh(true);
                return true;
            case R.id.menu_submissions_sortby_CONTROVERSIAL_month:
                App.getJumbleInfoObj().setmSortBy(SubredditSort.CONTROVERSIAL);
                App.getJumbleInfoObj().setmTimePeriod(TimePeriod.MONTH);
                refresh(true);
                return true;
            case R.id.menu_submissions_sortby_CONTROVERSIAL_year:
                App.getJumbleInfoObj().setmSortBy(SubredditSort.CONTROVERSIAL);
                App.getJumbleInfoObj().setmTimePeriod(TimePeriod.YEAR);
                refresh(true);
                return true;
            case R.id.menu_submissions_sortby_CONTROVERSIAL_alltime:
                App.getJumbleInfoObj().setmSortBy(SubredditSort.CONTROVERSIAL);
                App.getJumbleInfoObj().setmTimePeriod(TimePeriod.ALL);
                refresh(true);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /* Returned from SubmissionViewer*/
        if (requestCode == Constants.REQUESTCODE_GOTO_BIG_DISPLAY) {
            if (resultCode == RESULT_OK) {
                //mToolbar.setAlpha(1);
            }
        } else if (requestCode == Constants.REQUESTCODE_GOTO_SUBREDDIT_VIEWER) {
            if (resultCode == Constants.RESULT_OK_START_OVER) {
                mHomeEventListener.startOver();
            }
        } else if (requestCode == Constants.REQUESTCODE_GOTO_LOG_IN) {
            if (resultCode == RESULT_OK) {
                // User successfully logged in. Update the current user.
                // New username will be last element of our usernames list within tokenStore
                String newUsername =
                        App.getTokenStore().getUsernames().get(App.getTokenStore().getUsernames().size() - 1);
                updateCurrentUser(newUsername);
                // empty the subreddit stack since we're starting over with new user
                while (!App.getJumbleInfoObj().getmSubredditStack().isEmpty()) {
                    App.getJumbleInfoObj().getmSubredditStack().pop();
                }
                // empty subreddit paginator as well
                while (!App.getStackRedditPaginator().isEmpty()) {
                    App.getStackRedditPaginator().pop();
                }

                // update shared prefernces with a list of this user's subscriptions
                if (!Utils.isUserSubscriptionsStored(newUsername)) {
                    FetchUserSubscriptionsTask =
                            new Utils.FetchUserSubscriptionsAndStoreLocally(
                                    newUsername,
                                    new OnRedditTaskListener() {
                                        @Override
                                        public void onSuccess() {
                                            refresh(true);
                                        }

                                        @Override
                                        public void onFailure(String exceptionMessage) {
                                            //todo
                                        }
                                    }).execute();
                }
                // this shouldn't ever be the case since a new user will never have their subscriptions stored
                else {
                    refresh(true);
                }
            }
        }
    }

    public void cancelRunning() {
        if (FetchUserSubscriptionsTask != null) FetchUserSubscriptionsTask.cancel(true);
        if (UnsubscribeSubredditTask != null) UnsubscribeSubredditTask.cancel(true);
        if (SubscribeSubredditTask != null) SubscribeSubredditTask.cancel(true);
    }

    private void setupToolbar() {
        if (isAdded()) {
            mToolbar.setVisibility(View.VISIBLE);
            mToolbar.setAlpha(1);

            // Displaying Explore category
            if (mCurrExploreTitle != null) {
                mToolbar.setTitle(mCurrExploreTitle);
                //mToolbar.setTitleTextColor(getResources().getColor(R.color.colorAccentBlue));
                mToolbar.setTitleTextAppearance(getContext(), R.style.toolbar_title_text_explore);
                mToolbar.setSubtitle(Utils.makeTextCute(App.getJumbleInfoObj().getmSortBy().toString()));
            }
            // Displaying a subreddit
            else if (mCurrSubreddit != null) {
                mToolbar.setTitleTextAppearance(getContext(), R.style.toolbar_title_text_default);
                // displaying saved submissions
                if (Constants.REQUEST_SAVED.equalsIgnoreCase(mCurrSubreddit)) {
                    mToolbar.setTitle(getResources().getString(R.string.saved));
                }
                // displaying a subreddit
                else {
                    mToolbar.setTitle(getResources().getString(R.string.subreddit_prefix) + mCurrSubreddit);
                    mToolbar.setSubtitle(Utils.makeTextCute(App.getJumbleInfoObj().getmSortBy().toString()));
                }
            }
            // Displaying user's frontpage
            else {
                mToolbar.setTitle(getResources().getString(R.string.frontpage));
                mToolbar.setTitleTextAppearance(getContext(), R.style.toolbar_title_text_default);
                mToolbar.setSubtitle(Utils.makeTextCute(App.getJumbleInfoObj().getmSortBy().toString()));
            }


            // set hamburger menu icon if viewing a subreddit, back arrow if viewing submission
            ActionBar toolbar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (toolbar != null) {
                toolbar.setDisplayHomeAsUpEnabled(true);
                toolbar.setHomeAsUpIndicator(mCurrSubreddit == null ? R.drawable.ic_menu : R.drawable.ic_white_back_arrow);
            }
        }
    }

    private void unpackArgs() {
        // SharedPreferencesTokenStore tokenStore = App.getTokenStore();
        Bundle arguments = this.getArguments();
        try {
            if (arguments != null) {
                mInvalidateDataSource = (boolean) arguments.getBoolean(Constants.ARGS_INVALIDATE_DATASOURCE);
                mCurrSubreddit = (String) arguments.getString(Constants.ARGS_CURR_SUBREDDIT, null);
                mCurrExploreTitle = (String) arguments.getString(Constants.EXTRA_GOTO_EXPLORE_CATEGORY, null);
            }
            //  mCurrSubreddit = mRedditClient.getmRedditDataRequestObj().getmSubreddit();
        } catch (NullPointerException npe) {
            throw new NullPointerException("Null ptr exception trying to unpack arguments in " + TAG);
        }
    }

    /* Drawerlayout config to handle navigation views */
    private void setupDrawerLayout(View v) {
        mDrawerLayout = v.findViewById(R.id.drawer_layout);
        // Lock the left nav drawer unless we're home. This is to prevent unexpected behavior
        // such as what would happen if we logged in as a new user in the middle of some transaction.
        // FYI: We already have a "startOver()" method that starts us over from home in case we ever
        // want to handle that condition. Let's just avoid it for now.
        if (!(getActivity() instanceof ActivityHome)
                || getActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START);
        }
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                /*  R.drawable.ic_menu,  nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                //supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                //supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    /* Left drawer/navigation view */
    private void setupLeftNavView(View v) {

        mNavigationViewLeft = (NavigationView) v.findViewById(R.id.nav_view_left);

        /* expandable nav */
        expandableListView = v.findViewById(R.id.expandable_list_left);
        prepareLeftMenuData();
        populateExpandableList();
        // Expand usernames
        expandableListView.expandGroup(0);

        /* Navigation view menu */
        Menu navViewMenu = mNavigationViewLeft.getMenu();

        /* Navigation menu header*/
        View navViewHeader = mNavigationViewLeft.getHeaderView(0);
        setupLeftNavViewHeader(navViewHeader);

        /* Log out button */
        mButtonLogout = (TextView) v.findViewById(R.id.navbar_button_logout);
        // hide logout button if we're in Guest mode
        mButtonLogout.setVisibility(Utils.isUserlessSafe() ? View.GONE : View.VISIBLE);
        mButtonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                confirmLogout(App.getSharedPrefs().getString(
                        Constants.MOST_RECENT_USER,
                        Constants.USERNAME_USERLESS));
            }
        });
    }

    private void setupRightNavView(View v) {
        mNavigationViewRight = (NavigationView) v.findViewById(R.id.nav_view_right);
        mRightNavProgressBar = (ProgressBar) v.findViewById(R.id.navview_right_progress);

        // set up spinner(header)
        Spinner spinner = (Spinner) mNavigationViewRight.getHeaderView(0).findViewById(R.id.navview_right_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.navview_right_dropdown_array, R.layout.spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                String clickedItem = (String) adapterView.getItemAtPosition(pos);
                FrameLayout mRightNavErrorMessageContainer = v.findViewById(R.id.navview_right_error_container);

                switch (clickedItem) {
                    // This should happen on app launch
                    case "Explore":
                        mRightNavErrorMessageContainer.setVisibility(View.GONE);
                        mRightNavRecycleView.setVisibility(View.VISIBLE);
                        // set up all the "Explore" options.
                        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 2);
                        mRightNavRecycleView.setLayoutManager(gridLayoutManager);
                        mRightNavRecycleView.setHasFixedSize(true);
                        initExploreCatagories();
                        mRightNavRecycleView.setAdapter(new ExploreGridRecyclerAdapter());
                        return;
                    case "My Subreddits":
                        String currUsername = App.getSharedPrefs().getString(Constants.MOST_RECENT_USER, null);

                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
                        mRightNavRecycleView.setLayoutManager(linearLayoutManager);

                        // No subreddits to display if in Guest mode
                        if (currUsername == null || Constants.USERNAME_USERLESS.equalsIgnoreCase(currUsername)) {
                            mRightNavRecycleView.setVisibility(View.GONE);
                            mRightNavErrorMessageContainer.setVisibility(View.VISIBLE);
                        }
                        // Subscriptions aren't stored in shared preferences, go fetch (shouldn't
                        // happen since we always fetch and store subscriptions at log in or on user switch)
                        else if (!Utils.isUserSubscriptionsStored(currUsername)) {
                            mRightNavRecycleView.setVisibility(View.GONE);
                            mRightNavProgressBar.setVisibility(View.VISIBLE);

                            FetchUserSubscriptionsTask =
                                    new Utils.FetchUserSubscriptionsAndStoreLocally(currUsername, new OnRedditTaskListener() {
                                        @Override
                                        public void onSuccess() {
                                            mRightNavRecycleView.setAdapter(new MySubredditsRecyclerAdapter());
                                            mRightNavRecycleView.setVisibility(View.VISIBLE);
                                            mRightNavProgressBar.setVisibility(View.GONE);
                                            mRightNavErrorMessageContainer.setVisibility(View.GONE);
                                        }

                                        @Override
                                        public void onFailure(String exceptionMessage) {
                                            mRightNavRecycleView.setVisibility(View.GONE);
                                            mRightNavErrorMessageContainer.setVisibility(View.VISIBLE);
                                        }
                                    }).execute();
                        }
                        // Subscriptions already stored, no need to do anything but display
                        else {
                            mRightNavErrorMessageContainer.setVisibility(View.GONE);
                            mRightNavRecycleView.setAdapter(new MySubredditsRecyclerAdapter());
                        }
                        return;
                    default:
                        return;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private class ExploreItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private RoundedImageView mExploreImage;
        private TextView mExploreTitle;

        public ExploreItemViewHolder(View itemView) {
            super(itemView);
            mExploreImage = (RoundedImageView) itemView.findViewById(R.id.explore_image);
            mExploreTitle = (TextView) itemView.findViewById(R.id.explore_title);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
        }
    }

    private class ExploreGridRecyclerAdapter extends RecyclerView.Adapter<ExploreItemViewHolder> {
        public ExploreGridRecyclerAdapter() {
        }

        @NonNull
        @Override
        public ExploreItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.explore_grid_item, viewGroup, false);
            return new ExploreItemViewHolder(view);
        }

        @NonNull
        @Override
        public void onBindViewHolder(@NonNull final ExploreItemViewHolder exploreItemGridItem, int position) {
            String category = mExploreCatagoriesList.get(position);

            exploreItemGridItem.mExploreTitle.setText(category);
            String bgUri = "android.resource://com.sometimestwo.jumble/"
                    + mExploreCatagoriesMap.get(category).getBgDrawableId();
            GlideApp.load(Uri.parse(bgUri))
                    .apply(new RequestOptions()
                            .centerInside()
                            .diskCacheStrategy(DiskCacheStrategy.ALL))
                    .into(exploreItemGridItem.mExploreImage);

            exploreItemGridItem.mExploreImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String category = mExploreCatagoriesList.get(position);
                    List<String> subreddits = mExploreCatagoriesMap.get(category).getSubredditList();

                    StringBuilder sb = new StringBuilder();
                    for (String subreddit : subreddits) {
                        sb.append(subreddit).append('+');
                    }

                    String exploreURL = sb.substring(0, sb.length() - 1);
                    App.getJumbleInfoObj().getmSubredditStack().push(exploreURL);

                    Intent visitSubredditIntent = new Intent(getContext(), ActivitySubredditViewer.class);
                    visitSubredditIntent.putExtra(Constants.EXTRA_GOTO_SUBREDDIT, exploreURL);
                    visitSubredditIntent.putExtra(Constants.EXTRA_GOTO_EXPLORE_CATEGORY, category);
                    startActivityForResult(visitSubredditIntent, Constants.REQUESTCODE_GOTO_SUBREDDIT_VIEWER);
                    mDrawerLayout.closeDrawer(mNavigationViewRight);
                }
            });

            exploreItemGridItem.mExploreTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    exploreItemGridItem.mExploreImage.callOnClick();
                }
            });

            //Long click listeners for future use
           /* exploreItemGridItem.mExploreImage.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    return false;
                }
            });

            exploreItemGridItem.mExploreTitle.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    return false;
                }
            });*/
        }

        @Override
        public int getItemCount() {
            return mExploreCatagoriesList.size();
        }
    }

    private void initExploreCatagories() {
        //store these values in a list for convenience
        mExploreCatagoriesList = new ArrayList<String>();
        for (String category : Arrays.asList(getResources().getStringArray(R.array.explore_catagories))) {
            mExploreCatagoriesList.add(category);
        }
        // maps an explore category to an image file that will follow the following naming
        // convention: explore_bg_category, where category is the explore category
        mExploreCatagoriesMap = new HashMap<>();
        mExploreCatagoriesMap.put("Funny", new Explore(R.drawable.explore_bg_funny, Arrays.asList(getResources().getStringArray(R.array.explore_subreddits_funny))));
        mExploreCatagoriesMap.put("Awwwww", new Explore(R.drawable.explore_bg_aww, Arrays.asList(getResources().getStringArray(R.array.explore_subreddits_aww))));
        mExploreCatagoriesMap.put("Travel", new Explore(R.drawable.explore_bg_travel, Arrays.asList(getResources().getStringArray(R.array.explore_subreddits_travel))));
        mExploreCatagoriesMap.put("Meme", new Explore(R.drawable.explore_bg_meme, Arrays.asList(getResources().getStringArray(R.array.explore_subreddits_meme))));
        mExploreCatagoriesMap.put("GIFs", new Explore(R.drawable.explore_bg_gifs, Arrays.asList(getResources().getStringArray(R.array.explore_subreddits_gifs))));
        mExploreCatagoriesMap.put("Food", new Explore(R.drawable.explore_bg_food, Arrays.asList(getResources().getStringArray(R.array.explore_subreddits_food))));
        mExploreCatagoriesMap.put("Motivational", new Explore(R.drawable.explore_bg_motivational, Arrays.asList(getResources().getStringArray(R.array.explore_subreddits_motivational))));
        mExploreCatagoriesMap.put("Woah", new Explore(R.drawable.explore_bg_woah, Arrays.asList(getResources().getStringArray(R.array.explore_subreddits_woah))));
        mExploreCatagoriesMap.put("Design", new Explore(R.drawable.explore_bg_design, Arrays.asList(getResources().getStringArray(R.array.explore_subreddits_design))));
        mExploreCatagoriesMap.put("Art", new Explore(R.drawable.explore_bg_art, Arrays.asList(getResources().getStringArray(R.array.explore_subreddits_art))));
        mExploreCatagoriesMap.put("WTF", new Explore(R.drawable.explore_bg_wtf, Arrays.asList(getResources().getStringArray(R.array.explore_subreddits_wtf))));
        mExploreCatagoriesMap.put("Adventure", new Explore(R.drawable.explore_bg_adventure, Arrays.asList(getResources().getStringArray(R.array.explore_subreddits_adventure))));
        mExploreCatagoriesMap.put("Nature", new Explore(R.drawable.explore_bg_nature, Arrays.asList(getResources().getStringArray(R.array.explore_subreddits_nature))));


        if (!mHideNSFW) {
            for (String nsfwCategory : Arrays.asList(getResources().getStringArray(R.array.explore_categories_NSFW))) {
                mExploreCatagoriesList.add(nsfwCategory);
            }
            mExploreCatagoriesMap.put("NSFW", new Explore(R.drawable.explore_bg_nsfw, Arrays.asList(getResources().getStringArray(R.array.explore_subreddits_nsfw))));
        }
    }

    private void prepareLeftMenuData() {
        List<ExpandableMenuModel> childModelsList = new ArrayList<>();

        ExpandableMenuModel expandableMenuModel = new ExpandableMenuModel(
                getResources().getString(R.string.menu_accounts),
                true,
                true);
        headerList.add(expandableMenuModel);

        // Always offer Guest option
        ExpandableMenuModel childModel
                = new ExpandableMenuModel(
                Constants.USERNAME_USERLESS_PRETTY,
                false,
                false);
        childModelsList.add(childModel);

        // Fill account names
        for (String username : App.getTokenStore().getUsernames()) {
            // ignore Userless account
            if (!Constants.USERNAME_USERLESS.equalsIgnoreCase(username)) {
                childModel = new ExpandableMenuModel(username, false, false);
                childModelsList.add(childModel);
            }
        }

        // option to add new account
        childModel = new ExpandableMenuModel(
                getResources().getString(R.string.menu_add_account),
                false,
                false);
        childModelsList.add(childModel);
        childList.put(expandableMenuModel, childModelsList);


        // option to go to saved submissions if logged in
        if (!Utils.isUserlessSafe()) {
            expandableMenuModel = new ExpandableMenuModel(
                    getResources().getString(R.string.menu_saved_submissions),
                    true,
                    false);
            headerList.add(expandableMenuModel);
            childList.put(expandableMenuModel, null);
        }
        // option to go to subreddit
        expandableMenuModel = new ExpandableMenuModel(
                getResources().getString(R.string.menu_goto_subreddit),
                true,
                false);
        headerList.add(expandableMenuModel);
        childList.put(expandableMenuModel, null);

        // option to open settings
        expandableMenuModel = new ExpandableMenuModel(
                getResources().getString(R.string.menu_settings),
                true,
                false);
        headerList.add(expandableMenuModel);
        childList.put(expandableMenuModel, null);
    }

    private void populateExpandableList() {

        expandableListAdapter = new ExpandableListAdapter(getContext(), headerList, childList);
        expandableListView.setAdapter(expandableListAdapter);

        expandableListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {

                if (headerList.get(groupPosition).isGroup) {
                    if (!headerList.get(groupPosition).hasChildren) {
                        String clickedItemTitle = headerList.get(groupPosition).menuName;

                        // go to subreddit
                        if (getResources().getString(R.string.menu_goto_subreddit)
                                .equalsIgnoreCase(clickedItemTitle)) {

                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.TransparentDialog);
                            builder.setTitle("Enter subreddit:");

                            EditText input = new EditText(getContext());
                            input.setInputType(InputType.TYPE_CLASS_TEXT);
                            input.setTextColor(getResources().getColor(R.color.colorWhite));
                            builder.setView(input);
                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String requestedSubreddit = input.getText().toString();
                                    gotoSubreddit(requestedSubreddit);
                                }
                            });
                            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                            AlertDialog alertDialog = builder.create();

                            // button color setup
                            alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                                @Override
                                public void onShow(DialogInterface dialogInterface) {
                                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                            .setTextColor(getResources().getColor(R.color.colorWhite));
                                    alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                                            .setTextColor(getResources().getColor(R.color.colorWhite));
                                }
                            });
                            alertDialog.show();
                            alertDialog.getWindow().setLayout((6 * mScreenWidth) / 7, (4 * mScreenHeight) / 18);

                            mDrawerLayout.closeDrawer(mNavigationViewLeft);
                        } else if (getResources().getString(R.string.menu_settings)
                                .equalsIgnoreCase(clickedItemTitle)) {
                            mHomeEventListener.openSettings();
                        } else if (getResources().getString(R.string.menu_saved_submissions)
                                .equalsIgnoreCase(clickedItemTitle)) {
                            gotoSubreddit(Constants.REQUEST_SAVED);
                        }
                    }
                }
                return false;
            }
        });

        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent,
                                        View v,
                                        int groupPosition,
                                        int childPosition,
                                        long id) {
                if (childList.get(headerList.get(groupPosition)) != null) {
                    ExpandableMenuModel model = childList.get(headerList.get(groupPosition)).get(childPosition);
                    String clickedMenuItemName = model.menuName;
                    // A username in Accounts tab was clicked
                    if (App.getTokenStore().getUsernames().contains(clickedMenuItemName)
                            || Constants.USERNAME_USERLESS_PRETTY.equalsIgnoreCase(clickedMenuItemName)) {
                        // user has selected to switch to userless mode
                        if (Constants.USERNAME_USERLESS_PRETTY.equalsIgnoreCase(clickedMenuItemName)) {
                            new FetchUserlessAccountTask().execute();
                        }
                        // user selected a non-guest switch to
                        else {
                            // make sure we have a list of user subscriptions, else go fetch now
                            if (Utils.isUserSubscriptionsStored(clickedMenuItemName)) {
                                App.getAccountHelper().switchToUser(clickedMenuItemName);
                                switchOrLogoutCleanup(clickedMenuItemName);
                            } else {
                                FetchUserSubscriptionsTask =
                                        new Utils.FetchUserSubscriptionsAndStoreLocally(
                                                clickedMenuItemName,
                                                new OnRedditTaskListener() {
                                                    @Override
                                                    public void onSuccess() {
                                                        App.getAccountHelper().switchToUser(clickedMenuItemName);
                                                        switchOrLogoutCleanup(clickedMenuItemName);
                                                    }

                                                    @Override
                                                    public void onFailure(String exceptionMessage) {
                                                        //todo
                                                    }
                                                }).execute();
                            }
                        }
                    }
                    // clicked "Add account" to add a new acc
                    else if (getResources().getString(R.string.menu_add_account).equalsIgnoreCase(clickedMenuItemName)) {
                        Intent loginIntent = new Intent(getContext(), ActivityNewUserLogin.class);
                        //unlockSessionIntent.putExtra("REQUEST_UNLOCK_SESSION", true);
                        startActivityForResult(loginIntent, Constants.REQUESTCODE_GOTO_LOG_IN);
                    }
                }
                return false;
            }
        });
        expandableListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long id) {
                if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                    int groupPosition = ExpandableListView.getPackedPositionGroup(id);
                    int childPosition = ExpandableListView.getPackedPositionChild(id);

                    ExpandableMenuModel model = childList.get(headerList.get(groupPosition)).get(childPosition);
                    String longClickedItem = model.menuName;

                    // only care about handling long clicks for user names
                    if (App.getTokenStore().getUsernames().contains(longClickedItem)) {
                        confirmLogout(longClickedItem);
                    }

                    return true;
                }
                return false;
            }
        });
    }

    // "My Subreddits" option in right nav view
    private class MySubredditsRecyclerAdapter extends RecyclerView.Adapter<MySubredditsViewHolder> {

        ArrayListStringIgnoreCase listMySubreddits;

        public MySubredditsRecyclerAdapter() {
            this.listMySubreddits =
                    Utils.getSubscriptionsFromSharedPrefs(App.getSharedPrefs().getString(
                            Constants.MOST_RECENT_USER,
                            Constants.USERNAME_USERLESS));
        }

        @NonNull
        @Override
        public MySubredditsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.my_subreddits_recycler_item, viewGroup, false);
            return new MySubredditsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MySubredditsViewHolder holder, int position) {
            String subreddit = listMySubreddits.get(position);
            holder.mMySubredditText.setText(subreddit);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    gotoSubreddit(subreddit);
                }
            });
        }

        @Override
        public int getItemCount() {
            return listMySubreddits.size();
        }
    }


    private class MySubredditsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView mMySubredditText;

        public MySubredditsViewHolder(View itemView) {
            super(itemView);
            mMySubredditText = (TextView) itemView.findViewById(R.id.my_subreddits_text);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
        }
    }

    private void setupLeftNavViewHeader(View navViewHeader) {
        mNavViewHeaderTitle = (TextView) navViewHeader.findViewById(R.id.navview_header_text_title);
        String mostRecentUser = App.getSharedPrefs().getString(Constants.MOST_RECENT_USER, Constants.USERNAME_USERLESS);
        if (Constants.USERNAME_USERLESS.equalsIgnoreCase(mostRecentUser)) {
            mNavViewHeaderTitle.setText(Constants.USERNAME_USERLESS_PRETTY);
        } else {
            mNavViewHeaderTitle.setText(mostRecentUser);
        }
    }

    /*
        Hover preview set up. Hides any views that aren't needed and unhides
        the ones we need according to which viewer has been selected in settings.
     */
    private void setupPreviewer(SubmissionObj item) {
        /* Small previewer on hold for now*/
        /*if (mPreviewSize == Constants.HoverPreviewSize.SMALL) {
            mHoverPreviewContainerSmall.setVisibility(View.VISIBLE);
            mHoverPreviewContainerLarge.setVisibility(View.GONE);
            //popupWindow = new PopupWindow(customView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            if (item.getSubmissionType() == Constants.SubmissionType.IMAGE) {
                //mHoverPreviewTitleSmall.setText(item.getTitle());
                mHoverImagePreviewSmall.setVisibility(View.VISIBLE);
            }
            if (item.getSubmissionType() == Constants.SubmissionType.GIF) { // and video?

            }
        } else */
        if (mPreviewSize == Constants.HoverPreviewSize.LARGE) {
            if (mHoverPreviewContainerLarge.getParent() != null) {
                ((ViewGroup) mHoverPreviewContainerLarge.getParent()).removeView(mHoverPreviewContainerLarge);
            }
            // forcing the view to display over the entire screen
            ViewGroup vg = (ViewGroup) (getActivity().getWindow().getDecorView().getRootView());
            vg.addView(mHoverPreviewContainerLarge);

            mHoverPreviewContainerLarge.setVisibility(View.VISIBLE);
            // mHoverPreviewContainerSmall.setVisibility(View.GONE);
            mHoverPreviewMediaContainerLarge.setVisibility(View.VISIBLE);

            //v.redd.it links will always be non-image. Display in video view.
            if (item.getDomain() == Constants.SubmissionDomain.VREDDIT) {
                // hold off on dispalying videoview for now. Done when vreddit
                // loading task finished (onVRedditMuxTaskCompleted)
            } else {
                if (item.getSubmissionType() == Constants.SubmissionType.IMAGE
                        || item.getSubmissionType() == Constants.SubmissionType.ALBUM
                        || item.getSubmissionType() == null) {
                    focusMediaView(mHoverImagePreviewLarge);
                }
                if (item.getSubmissionType() == Constants.SubmissionType.GIF) {
                    //IREDDIT Gifs to be played in ImageView via Glide
                    if (item.getDomain() == Constants.SubmissionDomain.IREDDIT) {
                        focusMediaView(mHoverImagePreviewLarge);
                    }
                    // All other gifs to be played using Exoplayer
                    else {
                        focusMediaView(mExoplayerLarge);
                    }
                }
            }
        }
    }

    /* Shows one view, hides two. Used during switching between three different types of media viewers*/
    private void focusMediaView(View focused) {
        mPreviewerVideoViewLarge.setVisibility(focused == mPreviewerVideoViewLarge ? View.VISIBLE : View.GONE);
        mHoverImagePreviewLarge.setVisibility(focused == mHoverImagePreviewLarge ? View.VISIBLE : View.GONE);
        mExoplayerLarge.setVisibility(focused == mExoplayerLarge ? View.VISIBLE : View.GONE);
    }

    private void validatePreferences() throws Exception {
        if (prefs_settings != null) {
            mHideNSFW = prefs_settings.getBoolean(Constants.PREFS_HIDE_NSFW, true);
            mAllowImagePreview = prefs_settings.getBoolean(Constants.PREFS_ALLOW_HOVER_PREVIEW, true);
            mAllowClickClose = prefs_settings.getBoolean(Constants.PREFS_ALLOW_CLOSE_CLICK, true);
            mPreviewSize = prefs_settings.getString(Constants.PREFS_PREVIEW_SIZE, Constants.PREFS_PREVIEW_SIZE_LARGE)
                    .equalsIgnoreCase(Constants.PREFS_PREVIEW_SIZE_LARGE)
                    ? Constants.HoverPreviewSize.LARGE : Constants.HoverPreviewSize.SMALL;
            mDisplayDomainIcon = prefs_settings.getBoolean(Constants.PREFS_ALLOW_DOMAIN_ICON, false);
            mHideNSFWThumbs = prefs_settings.getBoolean(Constants.PREFS_HIDE_NSFW_THUMBS, false);
            mDisplayFiletypeIcons = prefs_settings.getBoolean(Constants.PREFS_ALLOW_FILETYPE_ICON, false);
            mDisplayNSFWIcon = prefs_settings.getBoolean(Constants.PREFS_SHOW_NSFW_ICON, false);

            App.getJumbleInfoObj().setHideNSFW(mHideNSFW);
        } else {
            throw new Exception("Failed to retrieve SharedPreferences on validatePreferences(). "
                    + "Could not find prefs_settings KEY_SHARED_PREFS.");
        }
    }

    private static DiffUtil.ItemCallback<SubmissionObj> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SubmissionObj>() {
                @Override
                public boolean areItemsTheSame(SubmissionObj oldItem, SubmissionObj newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(SubmissionObj oldItem, SubmissionObj newItem) {
                    return oldItem.equals(newItem);
                }
            };


    public class SubredditContentRecyclerAdapter
            extends PagedListAdapter<SubmissionObj, SubredditContentRecyclerAdapter.ItemViewHolder> {
        private Context mContext;
        private String firstSavedItemID;

        SubredditContentRecyclerAdapter(Context mContext) {
            super(DIFF_CALLBACK);
            this.mContext = mContext;
        }

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(
                    R.layout.recycler_item_thumbnail,
                    parent,
                    false);
            return new ItemViewHolder(view);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onBindViewHolder(@NonNull final ItemViewHolder holder, int position) {
            mProgressbarMain.setVisibility(View.GONE);
            // empty subreddit, nothing to display
            if (is404) return;

            // prevent null-ness from empty subreddit or any unexpected submission type errors
            SubmissionObj item = getItem(holder.getAdapterPosition()) == null
                    ? new SubmissionObj(Constants.FetchSubmissionsFlag.NOT_FOUND_404)
                    : getItem(holder.getAdapterPosition());

            // initialize anything that might overlay the thumbnail
            // to GONE to avoid conflicts when recycling
            holder.thumbnailIconNSFW.setVisibility(View.GONE);
            holder.thumbnailIconFiletype.setVisibility(View.GONE);
            holder.thumbnailIconDomain.setVisibility(View.GONE);

            // Workaround for checking if requested subreddit is empty (or invalid).
            // If invalid subreddit, there should exist only 1 element and this field will be true
            // is404 prevents more 404 pages from being added to the backstack
            if (item.isSubredditEmpty()) {
                is404 = true;
                holder.itemView.setVisibility(View.GONE);
                mHomeEventListener.set404(true);
                // the requested subreddit was empty - Display 404 page
                display404();
                return;
            } else if (item.getFetchSubmissionsFlag() == Constants.FetchSubmissionsFlag.START_OVER) {
                mHomeEventListener.startOver();
            }
            // assume reddit has not provided a thumbnail to be safe
            String thumbnail = Constants.URI_404_thumbnail;
            item.setSubmissionType(Utils.getSubmissionType(item.getUrl()));

            is404 = false;
            // Imgur
            //TODO: imgur albums. Example URL https://imgur.com/a/K8bJ9pV (nsfw)
            if (item.getDomain() == Constants.SubmissionDomain.IMGUR) {
                if (Utils.isImgurAlbum(item.getUrl())) {
                    item.setSubmissionType(Constants.SubmissionType.ALBUM);
                }
                // Check if submission type is null. This will happen if the item's URL is
                // to a non-direct IMAGE(not gif/video) link such as https://imgur.com/qTadRtq
                if (item.getSubmissionType() == null) {
                    // Here we assume indirect imgur links refer to images only
                    item.setSubmissionType(Constants.SubmissionType.IMAGE);
                }
                // We assume item will always have a thumbnail in an image format
                thumbnail = item.getThumbnail();
                holder.thumbnailIconDomain.setBackground(getResources().getDrawable(R.drawable.ic_imgur_i_black_bg));
            }
            //v.redd.it
            else if (item.getDomain() == Constants.SubmissionDomain.VREDDIT) {
                if ("hosted:video".equalsIgnoreCase(item.getPostHint())) {
                    item.setSubmissionType(Constants.SubmissionType.VIDEO);
                } else {
                    item.setSubmissionType(Constants.SubmissionType.GIF);
                }

                thumbnail = item.getThumbnail();
                holder.thumbnailIconDomain.setBackground(getResources().getDrawable(R.drawable.ic_reddit_blue_circle));
            }
            // i.redd.it
            else if (item.getDomain() == Constants.SubmissionDomain.IREDDIT) {
                thumbnail = item.getThumbnail();
                holder.thumbnailIconDomain.setBackground(getResources().getDrawable(R.drawable.ic_reddit_circle_orange));
            }
            //gfycat
            else if (item.getDomain() == Constants.SubmissionDomain.GFYCAT) {
                // Assume all Gfycat links are of submission type GIF
                item.setSubmissionType(Constants.SubmissionType.GIF);
                thumbnail = item.getThumbnail();
                holder.thumbnailIconDomain.setBackground(getResources().getDrawable(R.drawable.ic_gfycat_circle_blue));

            }
            //youtube
            else if (item.getDomain() == Constants.SubmissionDomain.YOUTUBE) {
                //item.setSubmissionType(Constants.SubmissionType.VIDEO);
                thumbnail = item.getThumbnail();
                holder.thumbnailIconDomain.setBackground(getResources().getDrawable(R.drawable.ic_youtube_red));
            }

            // Double check Reddit assigned a valid image (jpg,png,etc) as the thumbnail.
            if (Arrays.asList(Constants.VALID_IMAGE_EXTENSION)
                    .contains(Utils.getFileExtensionFromUrl(item.getThumbnail()))) {
                thumbnail = item.getThumbnail();
            }
            // If not, check if submission URL is valid image to use as thumbnail.
            else if (Arrays.asList(Constants.VALID_IMAGE_EXTENSION)
                    .contains(Utils.getFileExtensionFromUrl(item.getUrl()))) {
                thumbnail = item.getUrl();
            } else {
                // will assign 404 thumbnail if not given a thumbnail at this point
                item.setThumbnail(Constants.URI_404_thumbnail);
                thumbnail = Constants.URI_404_thumbnail;
                //holder.thumbnailIconDomain.setBackground(null);
            }

            /* Load thumbnail into recyclerview */
            // Check if we need to hide thumbnail (settings option)
            if (item.isNSFW() && mHideNSFWThumbs) {
                GlideApp.load(getResources().getDrawable(R.drawable.ic_reddit_nsfw_dark))
                        .apply(new RequestOptions().centerInside().diskCacheStrategy(DiskCacheStrategy.ALL))
                        .into(holder.thumbnailImageView);
            } else {
                GlideApp.load(thumbnail)
                        .listener(new RecyclerLoadProgressListener(item, holder))
                        .apply(new RequestOptions().centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL))
                        .into(holder.thumbnailImageView);
            }

            /* open submission viewer when image clicked*/
            holder.thumbnailImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    holder.onClick(holder.thumbnailImageView);
                }
            });

            /* Long press previewer */
            holder.thumbnailImageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View pView) {
                    // do nothing if previewing has been disabled through settings
                    if (!mAllowImagePreview) {
                        return true;
                    }
                    // prevent recyclerview from handling touch events, otherwise bad things happen
                    mRecyclerHome.setHandleTouchEvents(false);
                    isImageViewPressed = true;
                    /* Small previewer on hold for now. */
                    /*
                    if (mPreviewSize == Constants.HoverPreviewSize.SMALL) {
                        mHoverPreviewTitleSmall.setText(item.getTitle());
                        //popupWindow = new PopupWindow(customView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                        if (item.getSubmissionType() == Constants.SubmissionType.IMAGE) {
                            setupPreviewer(item);
                            GlideApp.load(item.getCleanedUrl() != null ? item.getCleanedUrl() : item.getUrl())
                                    .apply(new RequestOptions()
                                            .diskCacheStrategy(DiskCacheStrategy.ALL))
                                    .into(mHoverImagePreviewSmall);
                        } else if (item.getSubmissionType() == Constants.SubmissionType.GIF) {

                        }
                    } else*/

                    if (mPreviewSize == Constants.HoverPreviewSize.LARGE) {
                     /*   mHoverPreviewTitleLarge.setText(item.getCompactTitle() != null
                                ? item.getCompactTitle() : item.getTitle());
                        mHoverPreviewSubredditLarge.setText("/r/" + item.getSubreddit());*/
                        setupPreviewer(item);

                        if (item.getSubmissionType() == Constants.SubmissionType.IMAGE) {
                            // use cleanedURL if exists (might have already fetched this indirect image before)
                            String imgurUrl = item.getCleanedUrl() != null ? item.getCleanedUrl() : item.getUrl();
                            // Imgur Urls might be pointing to indirect image URLs
                            if (item.getDomain() == Constants.SubmissionDomain.IMGUR
                                    && !Arrays.asList(Constants.VALID_IMAGE_EXTENSION)
                                    .contains(Utils.getFileExtensionFromUrl(imgurUrl))) {
                                mHoverPreviewContainerLarge.setVisibility(View.GONE);
                                mPreviewerProgressBar.setVisibility(View.VISIBLE);
                                // fixes indirect imgur url and uses Glide to load image on success
                                Utils.fixIndirectImgurUrl(item, Utils.getImgurHash(item.getUrl()),
                                        new OnTaskCompletedListener() {
                                            @Override
                                            public void downloadSuccess() {
                                                if (getActivity() != null) {
                                                    getActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            mToolbar.setAlpha(.1f);
                                                            mHoverPreviewContainerLarge.setVisibility(View.VISIBLE);
                                                            mPreviewerProgressBar.setVisibility(View.GONE);
                                                            GlideApp.load(item.getCleanedUrl())
                                                                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                                                                    /* .listener(new GlideProgressListener(mPreviewerProgressBar))*/
                                                                    .into(mHoverImagePreviewLarge);
                                                        }
                                                    });
                                                } else {
                                                   /* Log.e(TAG,
                                                            "getActivity() null when trying to fixIndirectImgurUrl for url "
                                                                    + item.getUrl());*/
                                                }
                                            }

                                            @Override
                                            public void downloadFailure() {
                                                super.downloadFailure();
                                            }
                                        });
                            }
                            // image should be ready to be displayed here
                            else {
                                mToolbar.setAlpha(.1f);
                                GlideApp.load(item.getCleanedUrl() != null ? item.getCleanedUrl() : item.getUrl())
                                        .listener(new GlideProgressListener(mPreviewerProgressBar))
                                        .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                                        .into(mHoverImagePreviewLarge);
                            }
                        } else if (item.getSubmissionType() == Constants.SubmissionType.GIF
                                || item.getSubmissionType() == Constants.SubmissionType.VIDEO) {
                            // gif might have a .gifv (imgur) extension
                            // ...need to fetch corresponding .mp4
                            if (item.getDomain() == Constants.SubmissionDomain.IMGUR
                                    && Utils.getFileExtensionFromUrl(item.getUrl())
                                    .equalsIgnoreCase("gifv")
                                    && item.getCleanedUrl() == null) {
                                Utils.getMp4LinkImgur(item, Utils.getImgurHash(item.getUrl()), new OnTaskCompletedListener() {
                                    @Override
                                    public void downloadSuccess() {
                                        super.downloadSuccess();
                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    // cleaned url should contain .mp4 link
                                                    initializePreviewExoPlayer(item.getCleanedUrl());
                                                    mToolbar.setAlpha(.1f);
                                                }
                                            });
                                        } else {
                                          /*  Log.e(TAG,
                                                    "getActivity() null when trying getMp4LinkImgur for url "
                                                            + item.getUrl());*/
                                        }
                                    }

                                    @Override
                                    public void downloadFailure() {
                                        super.downloadFailure();
                                    }
                                });
                            }
                            // gifv URL that we've already fetched before
                            else if (item.getDomain() == Constants.SubmissionDomain.IMGUR
                                    && item.getCleanedUrl() != null) {
                                initializePreviewExoPlayer(item.getCleanedUrl());
                                mToolbar.setAlpha(.1f);
                            }
                            //GFYCAT
                            else if (item.getDomain() == Constants.SubmissionDomain.GFYCAT
                                    && item.getCleanedUrl() == null) {
                                // We're given a URL in this format: //https://gfycat.com/SpitefulGoldenAracari
                                // extract gfycat ID (looks like:SpitefulGoldenAracari)
                                String gfycatHash = Utils.getGfycatHash(item.getUrl());
                                // get Gfycat .mp4 "clean url"
                                Call<GfycatWrapper> gfycatObj = Utils.getGyfCatObjToEnqueue(gfycatHash);
                                gfycatObj.enqueue(new Callback<GfycatWrapper>() {
                                    @Override
                                    public void onResponse(Call<GfycatWrapper> call, Response<GfycatWrapper> response) {
                                        //Log.d(TAG, "onResponse: feed: " + response.body().toString());
                                       /* Log.d("GFYCAT_RESPONSE",
                                                "getGyfCatObjToEnqueue onResponse: Server Response: " + response.toString());*/

                                        GfyItem gfyItem = new GfyItem();
                                        try {
                                            gfyItem = response.body().getGfyItem();
                                        } catch (Exception e) {
                                           /* Log.e("GFYCAT_RESPONSE_ERROR",
                                                    "Failed in attempt to retrieve gfycat object for hash "
                                                            + gfycatHash + ". "
                                                            + e.getMessage());*/
                                            call.cancel();
                                        }
                                        if (gfyItem != null) {
                                            item.setCleanedUrl(gfyItem.getMobileUrl() != null
                                                    ? gfyItem.getMobileUrl() : gfyItem.getMp4Url());
                                            item.setMp4Url(gfyItem.getMp4Url());

                                            // Display gfycat
                                            initializePreviewExoPlayer(item.getCleanedUrl());
                                            mToolbar.setAlpha(.1f);
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<GfycatWrapper> call, Throwable t) {
                                        call.cancel();
                                        /*Log.e("GETGFYCAT_ERROR",
                                                "getGyfCatObjToEnqueue onFailure: Unable to retrieve Gfycat: " + t.getMessage());*/
                                    }

                                });
                            }
                            // VREDDIT videos are high maintance
                            else if (item.getDomain() == Constants.SubmissionDomain.VREDDIT) {
                                mPreviewerProgressBar.setVisibility(View.VISIBLE);
                                String url = item.getEmbeddedMedia().getRedditVideo().getFallbackUrl();
                                try {
                                    new Utils.FetchVRedditGifTask(getContext(), url, new OnVRedditTaskCompletedListener() {
                                        @Override
                                        public void onVRedditMuxTaskCompleted(Uri uriToLoad) {
                                            mToolbar.setAlpha(.1f);
                                            mPreviewerVideoViewLarge.setVideoURI(uriToLoad);
                                            focusMediaView(mPreviewerVideoViewLarge);
                                            mPreviewerVideoViewLarge.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                                @Override
                                                public void onPrepared(MediaPlayer mp) {
                                                    mp.start();
                                                    mp.setLooping(true);
                                                }
                                            });
                                            mPreviewerVideoViewLarge.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                                @Override
                                                public void onCompletion(MediaPlayer mp) {
                                                    mp.stop();
                                                    mp.release();
                                                }
                                            });
                                        }
                                    }).execute();
                                } catch (Exception e) {
                                    //LogUtil.e(e, "Error v.redd.it url: " + url);
                                }
                            }
                            // IREDDIT submissions will always be .gif (not .gifv)
                            // Also check for anything else that may be .gif here
                            else if (item.getDomain() == Constants.SubmissionDomain.IREDDIT
                                    || Utils.getFileExtensionFromUrl(item.getUrl()).equalsIgnoreCase("gif")) {
                                mToolbar.setAlpha(.1f);
                                mPreviewerProgressBar.setVisibility(View.VISIBLE);
                                GlideApp.asGif()
                                        .load(item.getUrl())
                                        .into(mHoverImagePreviewLarge);
                            }
                            // media has been fetched before and/or is ready to be played
                            else {
                                mToolbar.setAlpha(.1f);
                                initializePreviewExoPlayer(item.getCleanedUrl() != null ? item.getCleanedUrl() : item.getUrl());
                            }
                        }
                        // submission is of unknown type (i.e. submission from /r/todayilearned)
                        // TODO: or imgur album!!
                        else {
                            mToolbar.setAlpha(.1f);
                            mPreviewerProgressBar.setVisibility(View.GONE);
                            GlideApp.load(Constants.URI_404)
                                    .listener(new GlideProgressListener(mPreviewerProgressBar))
                                    .into(mHoverImagePreviewLarge);
                        }
                    }
                    return true;
                }
            });

            holder.thumbnailImageView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View pView, MotionEvent pEvent) {
                    boolean singleTap = mGestureDetector.onTouchEvent(pEvent);
                    // do nothing if previewing has been disabled through settings or if
                    // the touch we're detecting is a secondary touch that no one cares about
                    if (!mAllowImagePreview && !singleTap) {
                        return true;
                    }
                    // save ID of the first pointer(touch) ID
                    mActivePointerId = pEvent.getPointerId(0);

                    // find current touch ID
                    final int action = pEvent.getAction();
                    int currPointerId = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;

                    // only care about doing stuff that relates to first finger touch
                    if (currPointerId == mActivePointerId) {
                        if (pEvent.getAction() == MotionEvent.ACTION_UP) {
                            // hide hoverView on click release
                            if (isImageViewPressed) {
                                // done with hover view, allow recyclerview to handle touch events
                                mRecyclerHome.setHandleTouchEvents(true);
                                isImageViewPressed = false;
                              /*  if (mPreviewSize == Constants.HoverPreviewSize.SMALL) {
                                    mHoverPreviewContainerSmall.setVisibility(View.GONE);

                                } else*/
                                if (mPreviewSize == Constants.HoverPreviewSize.LARGE) {
                                    mPreviewerProgressBar.setVisibility(View.GONE);
                                    mHoverPreviewContainerLarge.setVisibility(View.GONE);
                                    clearVideoView();
                                    mHoverImagePreviewLarge.setVisibility(View.GONE);
                                    mExoplayerLarge.setVisibility(View.GONE);
                                    stopExoPlayer();
                                    // restore the toolbar
                                    mToolbar.setAlpha(1);
                                }
                            }
                        }
                    }
                    return false;
                }
            });

        }

        // Used for ensuring an icon (domain, nsfw, filetype - enabled through settings)
        // and the image being loaded are loaded together in the recycler thumbnail
        private class RecyclerLoadProgressListener implements RequestListener<Drawable> {
            // recyclerview's thumbnail
            private ItemViewHolder holder;
            private SubmissionObj item;

            public RecyclerLoadProgressListener(SubmissionObj item, final ItemViewHolder holder) {
                this.item = item;
                this.holder = holder;
            }

            @Override
            public boolean onLoadFailed(@Nullable GlideException e,
                                        Object model,
                                        Target<Drawable> target,
                                        boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource,
                                           Object model,
                                           Target<Drawable> target,
                                           com.bumptech.glide.load.DataSource dataSource,
                                           boolean isFirstResource) {
                if (getContext() != null) {
                    configureRecyclerThumbOverlay(item, holder);
                }
                return false;
            }
        }

        // todo:Release any resources that may be lingering after view has been recycled
        @Override
        public void onViewRecycled(@NonNull ItemViewHolder holder) {
            super.onViewRecycled(holder);
        }

        public class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            //TextView textView;
            ImageView thumbnailImageView;
            //YouTubeThumbnailView thumbnailYouTube;
            ImageView thumbnailIconDomain;
            ImageView thumbnailIconFiletype;
            ImageView thumbnailIconNSFW;

            public ItemViewHolder(View itemView) {
                super(itemView);
                thumbnailImageView = (ImageView) itemView.findViewById(R.id.thumbnail);
                //  thumbnailYouTube = (YouTubeThumbnailView) itemView.findViewById(R.id.recycler_youtube_thumbnail);
                thumbnailIconDomain = (ImageView) itemView.findViewById(R.id.thumbnail_domain_icon);
                thumbnailIconFiletype = (ImageView) itemView.findViewById(R.id.thumbnail_filetype_icon);
                thumbnailIconNSFW = (ImageView) itemView.findViewById(R.id.thumbnail_nsfw_icon);
            }

            @Override
            public void onClick(View view) {
                SubmissionObj submission = getItem(getLayoutPosition());
                openFullDisplayer(submission);
            }
        }
    }

    private void gotoSubreddit(String subreddit) {
        App.getJumbleInfoObj().getmSubredditStack().push(subreddit);

        Intent visitSubredditIntent = new Intent(getContext(), ActivitySubredditViewer.class);
        visitSubredditIntent.putExtra(Constants.EXTRA_GOTO_SUBREDDIT, subreddit);
        startActivityForResult(visitSubredditIntent, Constants.REQUESTCODE_GOTO_SUBREDDIT_VIEWER);
    }

    private void display404() {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment emptySubredditFragment = Fragment404.newInstance();

        /* Bundle args = new Bundle();
        args.putSerializable(Constants.EXTRA_SUBMISSION_OBJ, submission);
        mediaDisplayerFragment.setArguments(args);
        */

        int parentContainerId = ((ViewGroup) getView().getParent()).getId();
        ft.add(parentContainerId, emptySubredditFragment, Constants.TAG_FRAG_404);
        ft.addToBackStack(null);
        ft.commit();
    }

    /* Shows/hides icons which will be overlaid on the recyclerview's viewholder elements.
     *  Configured through Settings. */
    private void configureRecyclerThumbOverlay(SubmissionObj item,
                                               SubredditContentRecyclerAdapter.ItemViewHolder thumbnailHolder) {
        // thumbnailHolder.thumbnailIconFiletype.setVisibility(mDisplayFiletypeIcons ? View.VISIBLE : View.GONE);
        //thumbnailHolder.thumbnailIconDomain.setVisibility(mDisplayDomainIcon ? View.VISIBLE : View.GONE);
        // thumbnailHolder.thumbnailIconNSFW.setVisibility(mDisplayNSFWIcon ? View.VISIBLE : View.GONE);
        if (mDisplayDomainIcon) {
            thumbnailHolder.thumbnailIconDomain.setVisibility(View.VISIBLE);
        }

        /* Display file type icons if option is enabled through settings*/
        if (item.getSubmissionType() != null && mDisplayFiletypeIcons) {
            switch (item.getSubmissionType()) {
                case IMAGE:
                    thumbnailHolder.thumbnailIconFiletype.setBackground(
                            getResources().getDrawable(R.drawable.ic_filetype_image));
                    thumbnailHolder.thumbnailIconFiletype.setVisibility(View.VISIBLE);
                    break;
                case GIF:
                    thumbnailHolder.thumbnailIconFiletype.setBackground(
                            getResources().getDrawable(R.drawable.ic_filetype_gif));
                    thumbnailHolder.thumbnailIconFiletype.setVisibility(View.VISIBLE);
                    break;
                case VIDEO:
                    thumbnailHolder.thumbnailIconFiletype.setBackground(
                            getResources().getDrawable(R.drawable.ic_filetype_video));
                    thumbnailHolder.thumbnailIconFiletype.setVisibility(View.VISIBLE);
                    break;
                default:
                    break;
            }
        }

        // Display NSFW icon on top right if enabled through settings
        if (mDisplayNSFWIcon && item.isNSFW()) {
            thumbnailHolder.thumbnailIconNSFW.setVisibility(View.VISIBLE);
        } else {
            thumbnailHolder.thumbnailIconNSFW.setVisibility(View.GONE);
        }
    }

    private void openFullDisplayer(SubmissionObj submissionObj) {
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();

        Fragment bigDisplayFragment = FragmentFullDisplay.newInstance();
        Bundle args = new Bundle();
        args.putSerializable(Constants.ARGS_SUBMISSION_OBJ, submissionObj);
        bigDisplayFragment.setArguments(args);

        bigDisplayFragment.setTargetFragment(FragmentHome.this, Constants.REQUESTCODE_GOTO_BIG_DISPLAY);


        int parentContainerId = ((ViewGroup) getView().getParent()).getId();
        ft.add(parentContainerId, bigDisplayFragment, Constants.TAG_FRAG_FULL_DISPLAYER);
        ft.addToBackStack(null);
        ft.commit();
    }

    private void refresh(boolean invalidateData) {
        mHomeEventListener.refreshFeed(invalidateData);
    }

    private void invalidateData() {
        SubmissionsViewModel submissionsViewModel
                = ViewModelProviders.of(FragmentHome.this).get(SubmissionsViewModel.class);
        submissionsViewModel.invalidate();
    }

    // Prompts user to confirm log out and proceeds to remove account
    private void confirmLogout(String username) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.TransparentDialog);
        builder.setTitle("Confirm log out");
        builder.setMessage("Remove user " + username + "?");
        builder.setIcon(R.drawable.ic_white_log_out);
        builder.setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // log out, switch to userless, and clean up
                App.getTokenStore().deleteLatest(username);
                App.getTokenStore().deleteRefreshToken(username);
                new FetchUserlessAccountTask().execute();
                // remove this user's subscriptions from shared prefs
                Utils.removeLocalSubscriptionsList(username);
            }
        });
        builder.setNegativeButton(android.R.string.no, null);
        AlertDialog alertDialog = builder.create();

        // button color setup
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(getResources().getColor(R.color.colorWhite));
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setTextColor(getResources().getColor(R.color.colorWhite));
            }
        });

        // resize the alert dialog
        alertDialog.show();
        alertDialog.getWindow().setLayout((6 * mScreenWidth) / 7, (4 * mScreenHeight) / 18);
    }

    // Clean up any lingering views, media players, prefs_settings, etc
    private void switchOrLogoutCleanup(String newCurrUser) {
        mDrawerLayout.closeDrawer(mNavigationViewLeft);
        // Update most recent user to new user
        prefs_settings.edit().putString(Constants.MOST_RECENT_USER, newCurrUser).commit();

        // start our subreddit stack over cause we're starting over again
        while (!App.getJumbleInfoObj().getmSubredditStack().isEmpty()) {
            App.getJumbleInfoObj().getmSubredditStack().pop();
        }
        // empty subreddit paginator as well
        while (!App.getStackRedditPaginator().isEmpty()) {
            App.getStackRedditPaginator().pop();
        }

        //remove any sorting we had...null is ok :^)
        App.getJumbleInfoObj().setmSortBy(null);
        App.getJumbleInfoObj().setmTimePeriod(null);
        mHomeEventListener.startOver();
    }

    /* Updates SharedPreferences's current logged-in user. */
    private void updateCurrentUser(String newUsername) {
        try {
            App.getAccountHelper().switchToUser(newUsername);
        } catch (Exception e) {
            // Log.e(TAG, "Failed to switch to user while in updateCurrentUser() for username: " + newUsername);
        }
        // update the most recent logged in user in sharedprefs
        prefs_settings.edit().putString(Constants.MOST_RECENT_USER, newUsername).commit();
    }

    /*
     *   Hacky workaround for detecting single clicks. This was added to fix the issue where
     *   submissions would not open when clicked if "preview on long touch" option was disabled.
     *   (onTouchListener() was being called before onClick() every time.)
     */
    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            /*it needs to return true if we don't want
            to ignore rest of the gestures*/
            return true;
        }
    }

    /* Exoplayer for previewer*/

    private void initializePreviewExoPlayer(String url) {

        mExoplayerLarge.requestFocus();

        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);

        trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        player = ExoPlayerFactory.newSimpleInstance(getContext(), trackSelector);

        mExoplayerLarge.setPlayer(player);

        // hide the controller since we're dealing with previewer here and user will be long clicking
        mExoplayerLarge.hideController();
        mExoplayerLarge.setControllerVisibilityListener(new PlaybackControlView.VisibilityListener() {
            @Override
            public void onVisibilityChange(int i) {
                if (i == 0) {
                    mExoplayerLarge.hideController();
                }
            }
        });

        player.addListener(new PlayerEventListener());
        player.setPlayWhenReady(true);

        MediaSource mediaSource = new ExtractorMediaSource.Factory(mediaDataSourceFactory)
                .createMediaSource(Uri.parse(url));

        boolean haveStartPosition = currentWindow != C.INDEX_UNSET;
        if (haveStartPosition) {
            player.seekTo(currentWindow, playbackPosition);
        }

        // repeat mode: 0 = off, 1 = loop single video, 2 = loop playlist
        player.setRepeatMode(1);
        player.prepare(mediaSource, !haveStartPosition, false);
    }

    private class PlayerEventListener extends Player.DefaultEventListener {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            switch (playbackState) {
                case Player.STATE_IDLE:       // The player does not have any media to play yet.
                    //exoplayerProgressbar.setVisibility(View.VISIBLE);
                    break;
                case Player.STATE_BUFFERING:  // The player is buffering (loading the content)
                    //   exoplayerProgressbar.setVisibility(View.VISIBLE);
                    break;
                case Player.STATE_READY:      // The player is able to immediately play
                    // exoplayerProgressbar.setVisibility(View.GONE);
                    break;
                case Player.STATE_ENDED:      // The player has finished playing the media
                    //  exoplayerProgressbar.setVisibility(View.GONE);
                    break;
            }
        }
    }

    private void releaseExoPlayer() {
        if (player != null) {
            player.release();
        }
    }

    private void stopExoPlayer() {
        if (player != null) {
            player.stop();
        }
    }

    private void clearVideoView() {
        mPreviewerVideoViewLarge.stopPlayback();
        mPreviewerVideoViewLarge.setVisibility(View.GONE);
    }


    //* Sets our reddit client to userless and refreshes Home on completion*//*
    private class FetchUserlessAccountTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            App.getAccountHelper().switchToUserless();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            switchOrLogoutCleanup(Constants.USERNAME_USERLESS);
        }
    }
}
