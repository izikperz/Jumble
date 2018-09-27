package com.sometimestwo.moxie;

public interface HomeEventListener {
    public void openSettings();

    public void refreshFeed(boolean invalidateData);

    public void isHome(boolean isHome);

    // back arrow at top of screen (as opposed to hardware back button)
    public void menuGoBack();

    public void set404(boolean is404);
}
