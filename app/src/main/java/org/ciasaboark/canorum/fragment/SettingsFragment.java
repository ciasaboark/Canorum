package org.ciasaboark.canorum.fragment;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.ciasaboark.canorum.R;
import org.ciasaboark.canorum.fragment.preference.MainPreferenceFragment;

public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsFragment";
    private OnFragmentInteractionListener mListener;
    private View mView;
    private Toolbar mToolbar;

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_settings, menu);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_settings, container, false);
        initToolbar();
        initInnerPreferenceFragments();
        return mView;
    }

    private void initToolbar() {
        mToolbar = (Toolbar) mView.findViewById(R.id.local_toolbar);
        mToolbar.setTitle("Settings (TODO)");
        mToolbar.setBackgroundColor(getActivity().getResources().getColor(R.color.color_primary));
        mListener.setToolbar(mToolbar);
    }

    private void initInnerPreferenceFragments() {

        FragmentManager fm = getActivity().getFragmentManager();

        MainPreferenceFragment mainPreferenceFragment = MainPreferenceFragment.newInstance();

        fm.beginTransaction().add(R.id.pref_main, mainPreferenceFragment, "Main Prefs").commit();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


}
