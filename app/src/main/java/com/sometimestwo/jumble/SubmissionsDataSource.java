package com.sometimestwo.jumble;

import android.arch.paging.ItemKeyedDataSource;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sometimestwo.jumble.EventListeners.OnRedditTaskListener;
import com.sometimestwo.jumble.Model.SubmissionObj;
import com.sometimestwo.jumble.Model.JumbleInfoObj;
import com.sometimestwo.jumble.Utils.Constants;
import com.sometimestwo.jumble.Utils.Utils;

import net.dean.jraw.RedditClient;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.SubredditSort;
import net.dean.jraw.models.TimePeriod;
import net.dean.jraw.pagination.DefaultPaginator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SubmissionsDataSource extends ItemKeyedDataSource<String, SubmissionObj> {

    private final String TAG = SubmissionsDataSource.class.getSimpleName();
    // Info object that stores important browsing info such as current sortby and subreddit
    private JumbleInfoObj mJumbleInfoObj;
    // For paging through Reddit submission Listings
    //private DefaultPaginator<?> mPaginator;
    private boolean mIs404 = false;
    private boolean mEndOfSubreddit = false;

    public SubmissionsDataSource() {
        mJumbleInfoObj = App.getJumbleInfoObj();
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<String> params,
                            @NonNull final LoadInitialCallback<SubmissionObj> callback) {
        if (App.getAccountHelper().isAuthenticated()) {
            App.getStackRedditPaginator().push(buildNewPaginator());
            new FetchInitialSubmissionsTask(callback).execute();
        } else {
            new Utils.RedditReauthTask(new OnRedditTaskListener() {
                @Override
                public void onSuccess() {
                    App.getStackRedditPaginator().push(buildNewPaginator());
                    new FetchInitialSubmissionsTask(callback).execute();
                }

                @Override
                public void onFailure(String exceptionMessage) {
                    //todo
                }
            }).execute();
        }
    }

    // Shouldnt be needing this since we only ever append to our feed
    @Override
    public void loadBefore(@NonNull final LoadParams<String> params, @NonNull final LoadCallback<SubmissionObj> callback) {
    }

    // Loads the next page
    @Override
    public void loadAfter(@NonNull final LoadParams<String> params, @NonNull final LoadCallback<SubmissionObj> callback) {
        if (!mIs404 && !mEndOfSubreddit) {
            // make sure we're authenticated
            if (!App.getAccountHelper().isAuthenticated()) {
                mEndOfSubreddit = true;
               // Log.e("LOAD_AFTER_TEST", "----------Client not authenticated! Marked end of subreddit!");
            } else {
                new FetchSubmissionsTask(callback).execute();
            }
        }
    }

    @NonNull
    @Override
    public String getKey(@NonNull SubmissionObj item) {
        return item.getId();
    }


    private DefaultPaginator<?> buildNewPaginator() {
        RedditClient redditClient = App.getAccountHelper().getReddit();
        SubredditSort sortBy = mJumbleInfoObj.getmSortBy();
        TimePeriod timePeriod = mJumbleInfoObj.getmTimePeriod();

        DefaultPaginator.Builder<Submission, SubredditSort> builder;
        String subredditRequested = null;

        // Get the requested subreddit(s), if any
        if (!mJumbleInfoObj.getmSubredditStack().isEmpty()) {
            subredditRequested = mJumbleInfoObj.getmSubredditStack().peek();
        }
        // Requested subreddit can be either a name of a subreddit
        // or a special string indicating request for saved items
        if (subredditRequested != null) {
            if (subredditRequested.equalsIgnoreCase(Constants.REQUEST_SAVED)) {
                return redditClient.me()
                        .history("saved")
                        .limit(Constants.QUERY_PAGE_SIZE)
                        .build();
            }
            // We have a subreddit request - does not matter if logged in or not
            else {
                builder = redditClient.subreddit(subredditRequested).posts();
            }
        }
        // Logged in with no subreddit request - display user's front page
        else if (!redditClient.getAuthMethod().isUserless()) {
            builder = redditClient.frontPage();
        }
        // USERLESS and no subreddit request - display default
        else {
            StringBuilder sb = new StringBuilder();
            for (String subreddit : App.getJumbleInfoObj().getDefaultSubreddits()) {
                sb.append(subreddit).append("+");
            }
            // remove trailing +
            String defaultSubreddits = sb.toString().substring(0, sb.toString().length() - 1);
            builder = redditClient.subreddit(defaultSubreddits).posts();
        }


        return builder
                .limit(Constants.QUERY_PAGE_SIZE)
                .sorting(sortBy == null ? SubredditSort.BEST : sortBy)
                .timePeriod(timePeriod == null ? TimePeriod.DAY : timePeriod)
                .build();
    }

    /*
        Initial submissions loading. Uses callback to return result if success
     */
    private class FetchInitialSubmissionsTask extends AsyncTask<Void, Void, List<SubmissionObj>> {

        LoadInitialCallback<SubmissionObj> callback;

        FetchInitialSubmissionsTask(final LoadInitialCallback<SubmissionObj> callback) {
            this.callback = callback;
        }

        @Override
        protected List<SubmissionObj> doInBackground(Void... voids) {
            Listing<?> submissions;
            List<SubmissionObj> submissionObjs;
            try {
                submissions = App.getStackRedditPaginator().peek().next();
                /*  If we've retrieved an amount of pages less than our page size limit,
                 *  it's because the subreddit(s) in question are out of submissions to return.
                 *
                 *  There exists a condition that will break this: subreddit has exactly
                 *  Constants.QUERY_PAGE_SIZE amount of submissions, mEndOfSubreddit never gets
                 *  set to true, we try loading next page and we find nothing. Ignore this for now.
                 * */
                if (submissions.size() < Constants.QUERY_PAGE_SIZE) {
                    mEndOfSubreddit = true;
                }

                submissionObjs = mapSubmissions(submissions);
            } catch (Exception e) {
                // network issue
                //if (e instanceof UnknownHostException) {
                   /* mIs404 = true;
                    submissionObjs = new ArrayList<SubmissionObj>();
                    submissionObjs.add(new SubmissionObj(true));*/
                //  }
                //java.net.UnknownHostException: Unable to resolve host "www.reddit.com": No address associated with hostname

              /*  Log.e(SubmissionsDataSource.class.getSimpleName(),
                        " Failed to request initial submissions from reddit: " + e.getMessage());*/
                // Probably a network issue. Let's give user 404 page...

                // mapSubmissions will handle null as a 404 page
                return mapSubmissions(null);
            }
            return submissionObjs;
        }

        @Override
        protected void onPostExecute(List<SubmissionObj> submissions) {
            super.onPostExecute(submissions);
            callback.onResult(submissions);
        }
    }

    /*
        Non-initial submissions loading. Used to page through submissions in a subreddit after
        being initialized. This asyncronous function is what gets called as the recyclerview
        is scrolled. Uses callback to return result if success.
    */
    private class FetchSubmissionsTask extends AsyncTask<Void, Void, List<SubmissionObj>> {

        LoadCallback<SubmissionObj> callback;

        FetchSubmissionsTask(final LoadCallback<SubmissionObj> callback) {
            this.callback = callback;
        }

        @Override
        protected List<SubmissionObj> doInBackground(Void... voids) {
            Listing<?> submissions;
            List<SubmissionObj> submissionObjs = null;
            try {
                // get the next few submissions
                submissions = App.getStackRedditPaginator().peek().next();
                submissionObjs = mapSubmissions(submissions);
            } catch (Exception e) {
                // todo: catch network error such as timeout using similar techniques as above
               /* Log.e(SubmissionsDataSource.class.getSimpleName(),
                        " Failed to request non-initial submissions from reddit: " + e.getMessage());*/
            }
            return submissionObjs;
        }

        @Override
        protected void onPostExecute(List<SubmissionObj> submissions) {
            super.onPostExecute(submissions);
            callback.onResult(submissions);
        }
    }

    private List<SubmissionObj> mapSubmissions(Listing<?> submissionsAsListing) {
        // We might encounter non-submissions (saved comments). Filter first.
        List<Submission> submissionsList = filterNonSubmissions(submissionsAsListing);
        List<SubmissionObj> res = new ArrayList<>();

        if (submissionsList == null || submissionsList.size() < 1) {
            // The requested subreddit has 0(non-existant) posts
            res.add(new SubmissionObj(Constants.FetchSubmissionsFlag.NOT_FOUND_404));
            mIs404 = true;
            return res;
        }
        //mIs404 = false;
        for (Submission submission : submissionsList) {
            // filter some submissions out here
            if (isFilteredSubmission(submission)) {
               // Log.e("FILTER_TEST", "filtering submission with url: " + submission.getUrl());
                continue;
            }

            SubmissionObj s = new SubmissionObj();
            s.setAuthor(submission.getAuthor());
            s.setUrl(submission.getUrl());
            s.setId(submission.getId());
            s.setTitle(submission.getTitle());
            s.setCommentCount(submission.getCommentCount());
            s.setDateCreated(submission.getCreated());
            s.setFullName(submission.getFullName());
            s.setGilded(submission.getGilded());
            s.setHasThumbnail(submission.hasThumbnail());
            s.setHidden(submission.isHidden());
            s.setScoreHidden(submission.isScoreHidden());
            s.setHidden(submission.isHidden());
            s.setLinkFlairText(submission.getLinkFlairText());
            s.setPermalink(submission.getPermalink());
            s.setNSFW(submission.isNsfw());
            s.setLocked(submission.isLocked());
            s.setPostHint(submission.getPostHint());
            s.setRemoved(submission.isRemoved());
            s.setSelfPost(submission.isSelfPost());
            s.setSpam(submission.isSpam());
            s.setSpoiler(submission.isSpoiler());
            s.setSelfText(submission.getSelfText());
            s.setSubreddit(submission.getSubreddit());
            s.setSuggestedSort(submission.getSuggestedSort());
            s.setSubredditFullName(submission.getSubredditFullName());
            s.setVote(submission.getVote());
            s.setVisited(submission.isVisited());
            s.setScore(submission.getScore());
            s.setEmbeddedMedia(submission.getEmbeddedMedia());
            s.setSaved(submission.isSaved());
            s.setPreviewUrl(submission.getPreview() == null ?
                    null : submission.getPreview().getImages().get(0).getSource().getUrl());

            // Reddit can give us a few different non-image link strings as thumbnails.
            // Check for these and default to the submission URL if we've been provided
            // an invalid thumbnail.
            if (!submission.hasThumbnail()
                    || submission.getThumbnail() == null
                    || "image".equalsIgnoreCase(submission.getThumbnail())
                    || "nsfw".equalsIgnoreCase(submission.getThumbnail())
                    || "spoiler".equalsIgnoreCase(submission.getThumbnail())
                    || "default".equalsIgnoreCase(submission.getThumbnail())
                    || submission.getThumbnail().length() < 1) {
                // Make sure that if we're about to try using the submission URL as a thumbnail
                // that it's actually something we can display as a thumbnail
                if (Arrays.asList(Constants.VALID_IMAGE_EXTENSION)
                        .contains(Utils.getFileExtensionFromUrl(submission.getThumbnail()))) {
                    s.setThumbnail(submission.getUrl());
                }
            }
            // Reddit gave us an image as a thumbnail
            else {
                s.setThumbnail(submission.getThumbnail());
            }

            // domain
            if (submission.getDomain().contains("imgur")) {
                s.setDomain(Constants.SubmissionDomain.IMGUR);
            } else if (submission.getDomain().contains("v.redd.it")) {
                s.setDomain(Constants.SubmissionDomain.VREDDIT);
            } else if (submission.getDomain().contains("i.redd.it")) {
                s.setDomain(Constants.SubmissionDomain.IREDDIT);
            } else if (submission.getDomain().contains("gfycat")) {
                s.setDomain(Constants.SubmissionDomain.GFYCAT);
            } else if (submission.getDomain().contains("youtube")
                    || submission.getDomain().contains("youtu.be")) {
                s.setDomain(Constants.SubmissionDomain.YOUTUBE);
            } else {
                s.setDomain(Constants.SubmissionDomain.OTHER);
            }

            // add shortened title for displaying purposes if needed
            if (submission.getTitle().length() > Constants.MAX_TITLE_LENGTH) {
                s.setCompactTitle(submission.getTitle().substring(0,
                        Constants.MAX_TITLE_LENGTH) + "...");
            }

            res.add(s);
        }
        return res;
    }

    // JRAW will give us a Listing that can be different things. We only care about submissions.
    // Note: This was added to filter saved comments in particular
    private List<Submission> filterNonSubmissions(Listing<?> listing) {
        if (listing == null) return null;
        List<Submission> submissionsOnly = new ArrayList<Submission>();

        for (int i = 0; i < listing.size(); i++) {
            Object listingObj = listing.get(i);
            if (listingObj instanceof net.dean.jraw.models.Submission) {
                submissionsOnly.add((Submission) listingObj);
            }
        }
        return submissionsOnly;
    }

    // Messy way of determining which submissions to filter out.
    // Will return true if submissions needs to be filtered out, false otherwise
    private boolean isFilteredSubmission(Submission submission) {
        // self posts and NSFW (with NSFW filter enabled) always get filtered
        if (submission.isSelfPost() || submission.isNsfw() && mJumbleInfoObj.isHideNSFW())
            return true;

        // These domains will never need to be filtered
        if (submission.getDomain().contains("imgur")
                || submission.getDomain().contains("gfycat")
                || submission.getDomain().contains("youtube")
                || submission.getDomain().contains("youtu.be")
                || (submission.getDomain().contains("v.redd.it") && submission.getEmbeddedMedia() != null))
            return false;

        // "Optimize" filter (enabled through settings)
        return (App.getSharedPrefs().getBoolean(Constants.PREFS_FILTER_OPTIMIZE, true)
                && !Arrays.asList(Constants.VALID_MEDIA_EXTENSION).contains(Utils.getFileExtensionFromUrl(submission.getUrl())));
        //https://cdna.artstation.com/p/assets/images/images/013/430/078/large/taras-susak-n.jpg?1539584659
    }
}
