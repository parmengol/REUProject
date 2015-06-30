package edu.fiu.mpact.reuproject;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;


public class SelectMap extends BaseActivity {


    ListView list;

    String[] maps = {
            "Engineering 1st Floor", "Engineering 2nd Floor",
            "Engineering 3rd Floor"
            }; // map titles
    Integer[] imageId = {
            R.drawable.ec_1,
            R.drawable.ec_2,
            R.drawable.ec_3,  // list of the maps
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_map);

        CustomList adapter = new CustomList(SelectMap.this, maps, imageId);
        list=(ListView)findViewById(R.id.map_list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            Uri imageUri;

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                imageUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                                getResources().getResourcePackageName(imageId[+position]) + '/' +
                                getResources().getResourceTypeName(imageId[+position]) + '/' +
                                getResources().getResourceEntryName(imageId[+position]));


                final Intent data = new Intent();
                data.putExtra(Utils.Constants.MAP_NAME_EXTRA, maps[+position]);
                data.putExtra(Utils.Constants.MAP_URI_EXTRA, imageUri.toString());

                setResult(RESULT_OK, data);

               finish();


            }
        });


    }

}


