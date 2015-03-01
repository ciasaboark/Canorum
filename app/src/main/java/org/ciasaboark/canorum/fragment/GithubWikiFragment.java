/*
 * Copyright (c) 2015, Jonathan Nelson
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.ciasaboark.canorum.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;

import org.ciasaboark.canorum.R;

public class GithubWikiFragment extends Fragment {
    private static final String TAG = "GithubWikiFragment";

    private OnFragmentInteractionListener mListener;
    private View mView;
    private WebView mWebView;
    private Toolbar mToolbar;
    private ProgressBar mProgressBar;
    private String mWikiUrl = "https://github.com/ciasaboark/Canorum/wiki";
    private ImageView mBackButton;
    private ImageView mRefreshButton;
    private ImageView mHomeButtom;

    public GithubWikiFragment() {
        // Required empty public constructor
    }

    public static GithubWikiFragment newInstance() {
        GithubWikiFragment fragment = new GithubWikiFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_github_wiki, container, false);
        mToolbar = (Toolbar) mView.findViewById(R.id.local_toolbar);
        mToolbar.setTitle("Wiki");
        mToolbar.setNavigationIcon(R.drawable.ic_keyboard_backspace_white_24dp);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        mProgressBar = (ProgressBar) mView.findViewById(R.id.wiki_progress);
        mProgressBar.setProgress(0);

        mWebView = (WebView) mView.findViewById(R.id.wiki_webview);
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                mProgressBar.setProgress(progress);
            }
        });

        mWebView.setWebViewClient(new WikiWebView());
        mWebView.setHorizontalScrollbarOverlay(true);
        mWebView.loadUrl(mWikiUrl);

        initControls();

        return mView;
    }

    private void initControls() {
        mBackButton = (ImageView) mView.findViewById(R.id.wiki_controls_back);
        mRefreshButton = (ImageView) mView.findViewById(R.id.wiki_controls_refresh);
        mHomeButtom = (ImageView) mView.findViewById(R.id.wiki_controls_home);

        mBackButton.setEnabled(false);

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.goBack();
            }
        });

        mHomeButtom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.loadUrl(mWikiUrl);
            }
        });

        mRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.reload();
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private class WikiWebView extends WebViewClient {
        private String errorText = "<html><body><p>Could not load data, check your internet connection.</p></body></html>";
        private boolean lastLoadSuccess = true;
        private String lastLoadUrl = "";

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            String lowerUrl = url.toLowerCase();
            String lowerBase = mWikiUrl.toLowerCase();

            if (lowerUrl.contains(lowerBase)) {
                return false;
            } else {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
                return true;
            }
        }


        @Override
        public void onPageFinished(WebView view, String url) {
            Log.d(TAG, "Finished loading URL: " + url);
            mProgressBar.setProgress(0);
            mBackButton.setEnabled(view.canGoBack());
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Log.e(TAG, "Error: " + description);
            view.loadData(errorText, "text/html", "UTF-8");
        }
    }
}
