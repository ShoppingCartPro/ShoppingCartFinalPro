package com.hsfb.ullauri.finalproject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;

public class ItemListFragment extends Fragment {

    // Table used to contain textviews
    TableLayout shoppingCart;

    // ArrayList used to construct table rows
    ArrayList<String> itemTitle = new ArrayList<>();

    // Interface
    Delete itemDeleter;


    public ItemListFragment() {
    }

    // Interface used to pass deleteItem click response
    public interface Delete{
         void deleteRequest(String title);
    }

    // initializes the fragment interface from corresponding Activity
    public void onAttach(Activity activity){
        super.onAttach(activity);
        itemDeleter = (Delete) activity;
    }

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getArguments();
        itemTitle = bundle.getStringArrayList("ITEMTITLE");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        shoppingCart = new TableLayout(getActivity());
        shoppingCart.setLayoutParams(new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.MATCH_PARENT
        ));
        shoppingCart.setBackgroundColor(Color.WHITE);
        return shoppingCart;
    }

    // creates rows, assigns textview with itemtitle, and assigns them to table
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        if(itemTitle != null) {
            for (int i = 0; i < itemTitle.size(); i++) {
                TableRow row = new TableRow(getActivity().getApplicationContext());

                row.setLayoutParams(new TableRow.LayoutParams(
                        TableRow.LayoutParams.MATCH_PARENT,
                        TableRow.LayoutParams.WRAP_CONTENT
                ));

                final TextView title = new TextView(getActivity().getApplicationContext());
                final String item = itemTitle.get(i);
                title.setText(item);
                title.setTextSize(20);
                title.setTextColor(Color.BLACK);

                TableRow.LayoutParams details = new TableRow.LayoutParams(
                        TableRow.LayoutParams.MATCH_PARENT,
                        TableRow.LayoutParams.WRAP_CONTENT
                );

                details.setMargins(20, 0, 0, 0);
                title.setLayoutParams(details);
                row.addView(title);

                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        itemDeleter.deleteRequest(item);
                    }
                });

                shoppingCart.addView(row, new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.MATCH_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT
                ));

            }
        }
    }

}
