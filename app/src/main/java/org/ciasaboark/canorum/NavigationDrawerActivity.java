//package org.ciasaboark.canorum;
//
//import android.content.Intent;
//import android.graphics.Color;
//import android.os.Bundle;
//import android.provider.ContactsContract;
//import android.widget.Toast;
//
//import it.neokree.materialnavigationdrawer.MaterialAccount;
//import it.neokree.materialnavigationdrawer.MaterialAccountListener;
//import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
//import it.neokree.materialnavigationdrawer.MaterialSection;
//import it.neokree.materialnavigationdrawer.MaterialSectionListener;
//
///**
// * Created by Jonathan Nelson on 1/16/15.
// */
//public class NavigationDrawerActivity extends MaterialNavigationDrawer implements MaterialAccountListener {
//
//    MaterialSection section1, section2, recorder, night, last, settingsSection;
//
//    @Override
//    public void init(Bundle savedInstanceState) {
//
//        // add first account
//        MaterialAccount account = new MaterialAccount("NeoKree","neokree@gmail.com",this.getResources().getDrawable(R.drawable.android_music_player_end),this.getResources().getDrawable(R.drawable.android_music_player_rand));
//        this.addAccount(account);
//
//        // set listener
//        this.setAccountListener(this);
//
//        // create sections
//        section1 = this.newSection("Section 1",new FragmentIndex());
//        section2 = this.newSection("Section 2",new MaterialSectionListener() {
//            @Override
//            public void onClick(MaterialSection section) {
//                Toast.makeText(NavigationDrawerActivityRipple.this, "Section 2 Clicked", Toast.LENGTH_SHORT).show();
//
//                // deselect section when is clicked
//                section.unSelect();
//            }
//        });
//        // recorder section with icon and 10 notifications
//        recorder = this.newSection("Recorder",this.getResources().getDrawable(R.drawable.ic_mic_white_24dp),new FragmentIndex()).setNotifications(10);
//        // night section with icon, section color and notifications
//        night = this.newSection("Night Section", this.getResources().getDrawable(R.drawable.ic_hotel_grey600_24dp), new FragmentIndex())
//                .setSectionColor(Color.parseColor("#2196f3"), Color.parseColor("#1565c0")).setNotifications(150);
//        // night section with section color
//        last = this.newSection("Last Section", new FragmentButton()).setSectionColor(Color.parseColor("#ff9800"),Color.parseColor("#ef6c00"));
//
//        Intent i = new Intent(this,ContactsContract.Profile.class);
//        settingsSection = this.newSection("Settings",this.getResources().getDrawable(R.drawable.ic_settings_black_24dp),i);
//
//        // add your sections to the drawer
//        this.addSection(section1);
//        this.addSection(section2);
//        this.addSubheader("Subheader");
//        this.addSection(recorder);
//        this.addSection(night);
//        this.addDivisor();
//        this.addSection(last);
//        this.addBottomSection(settingsSection);
//
//        this.setBackPattern(MaterialNavigationDrawer.BACKPATTERN_BACK_TO_FIRST);
//    }
//
//    @Override
//    public void onAccountOpening(MaterialAccount account) {
//        // open profile activity
//        Intent i = new Intent(this,ContactsContract.Profile.class);
//        startActivity(i);
//    }
//
//    @Override
//    public void onChangeAccount(MaterialAccount newAccount) {
//        // when another account is selected
//    }
//}