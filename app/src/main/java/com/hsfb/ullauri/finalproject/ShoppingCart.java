package com.hsfb.ullauri.finalproject;

import android.app.Activity;
import android.app.AlertDialog;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.net.Uri;

import android.os.AsyncTask;
import android.os.Bundle;

import android.view.Gravity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


public class ShoppingCart extends Activity implements ItemListFragment.Delete {
    // ZXing scanner command
    static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
    // UPC database API link
    static final String url = "http://api.upcdatabase.org/json/ba263d6f28761575aa812ab96ebbf159/0";

    private ScrollView shoppingScrollView;
    private TextView getBudgetTextView;

    // values passed to construct ItemListFragment
    private ArrayList<String> itemTitle;
    Bundle bundle;

    private String wallet;
    private String upcCode;
    private String searchResult;
    private String currentTitle;

    // used to restore dialog boxes
    private boolean flagAddingItem;
    private boolean flagScanSearch;
    private boolean flagDeletingItem;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_cart);

        // gets scrollview container and text view used for budget
        shoppingScrollView = (ScrollView) findViewById(R.id.shoppingScrollView);
        getBudgetTextView = (TextView) findViewById(R.id.getBudgetTextView);

        // assigns the value passed from EnterBudget to text view
        Intent enterBudget = getIntent();
        wallet = enterBudget.getStringExtra(EnterBudget.EXTRA_ENTEREDBUDGET);
        getBudgetTextView.setText(wallet);

        // initialize
        itemTitle = new ArrayList<>();
        bundle = new Bundle();

        // if onCreate is called for the first time
        if (savedInstanceState == null) {
            // creates fragment and adds it to scrollview container
            Fragment itemListFragment = new ItemListFragment();
            FragmentManager fragMag = getFragmentManager();
            FragmentTransaction fragTrans = fragMag.beginTransaction();
            itemListFragment.setArguments(bundle);
            fragTrans.add(R.id.shoppingScrollView, itemListFragment).commit();
        }

    }

    public void toastCreator(String message) {
        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    // Called if user does not have the Zxing Barcode Scanner app (Download App?)
    public AlertDialog downloadScanDialog(final Activity act, CharSequence title, CharSequence message, CharSequence buttonYes, CharSequence buttonNo) {
        flagScanSearch = true;
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(act);
        downloadDialog.setTitle(title);
        downloadDialog.setMessage(message);
        // If user hits yes it tries to find the app for you to download
        downloadDialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                flagScanSearch = false;
                Uri uri = Uri.parse("market://search?q=pname:" + "com.google.zxing.client.android");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    act.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    toastCreator("ERROR CANT FIND SCANNER \n CHECK FOR UPDATES");
                }
            }
        });
        // If not user cant continue
        downloadDialog.setNegativeButton(buttonNo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                flagScanSearch = false;
                toastCreator("ERROR YOU MUST DOWNLOAD A BARCODE SCANNER");
            }
        });
        return downloadDialog.show();
    }

    // scan method assigned to "Scan" button in xml (onClick: )
    public void scanItem(View view) {
        try {
            Intent scan = new Intent(ACTION_SCAN);
            startActivityForResult(scan, 0);

        } catch (ActivityNotFoundException noScanner) {
            downloadScanDialog(ShoppingCart.this, "No Scanner Installed", "Download a Bar Code Scanner?", "Yes", "No").show();
        }
    }

    // receives result from scanItem using result code
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            // if successful then assigns upc value and fires a search
            if (resultCode == RESULT_OK) {
                upcCode = intent.getStringExtra("SCAN_RESULT");
                new JsonSearchTask().execute();
            }
        }
    }

    // connects to the upc data base link
    public String searchItem(String upcCode) {
        StringBuilder search = new StringBuilder();

        try {
            String query = url + upcCode;

            URL url = new URL(query);

            // launches connection
            HttpURLConnection searchQuery = (HttpURLConnection) url.openConnection();

            if (searchQuery.getResponseCode() == HttpURLConnection.HTTP_OK) {
                // reads byte stream
                InputStreamReader input = new InputStreamReader(searchQuery.getInputStream());
                // reads lines
                BufferedReader reader = new BufferedReader(
                        input);

                String line = null;
                while ((line = reader.readLine()) != null) {
                    search.append(line);
                }

                reader.close();
            }

        } catch (MalformedURLException a) {
            a.printStackTrace();
        } catch (IOException b) {
            b.printStackTrace();
        }

        return search.toString();
    }

    // parses the JSONObject returned by the UPC API
    public String parseSearch(String search) throws JSONException {
        StringBuilder results = new StringBuilder();

        JSONObject json = new JSONObject(search);
        // retrieves name and price
        results.append("Item Name:" + json.getString("itemname"));
        results.append("Price:" + json.getString("avg_price"));

        return results.toString();
    }

    // fires the search and parse in background and returns item name and value
    private class JsonSearchTask extends AsyncTask<Void, Void, Void> {

        // connects,searches,parses JSONobject
        @Override
        protected Void doInBackground(Void... arg0) {

            try {
                searchResult = parseSearch(searchItem(upcCode));
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return null;
        }

        // handles returned value
        @Override
        protected void onPostExecute(Void result) {

            // if item was found
            if (searchResult.length() > 0) {
                int nameEnd = searchResult.indexOf("Price");
                String itemName = searchResult.substring(10, nameEnd);
                Double itemCost = Double.parseDouble(searchResult.substring(16 + itemName.length()));

                // if cost is below available money: calls add dialog prompt box
                if (itemCost < Double.parseDouble(wallet)) {
                    String walletPreview = String.format("%.2f", Double.parseDouble(wallet) - itemCost);
                    addDialog(ShoppingCart.this, itemName, "DO YOU WANT TO ADD THIS ITEM?" + "\nMONEY LEFT IN WALLET AFTER PURCHASE: " + walletPreview, "YES", "NO", itemName, itemCost).show();
                } else {
                    toastCreator("EXCEEDS YOUR BUDGET");
                }
            } else {
                toastCreator("ITEM NOT FOUND");
            }

            super.onPostExecute(result);
        }

    }

    // Do you want to add this item
    public AlertDialog addDialog(final Activity act, CharSequence title, CharSequence message, CharSequence buttonYes, CharSequence buttonNo, final String itemName, final double itemCost) {
        flagAddingItem = true;
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(act);
        downloadDialog.setTitle(title);
        downloadDialog.setMessage(message);

        // calls addItem if user hits yes
        downloadDialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                flagAddingItem = false;
                addItem(itemName, itemCost, false);
            }
        });

        downloadDialog.setNegativeButton(buttonNo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                flagAddingItem = false;
                toastCreator("SCAN ANOTHER ITEM");
            }
        });
        return downloadDialog.show();
    }

    // adds item to ArrayList, constructs fragment, and replaces previous one
    public void addItem(final String itemName, final double itemCost, boolean restore) {
        // creates itemTitle and fragment
        String title = String.format(itemName + ":   " + "%.2f", itemCost);
        Fragment itemListFragment = new ItemListFragment();

        // adds item to arraylist assigns it to a bundle and passes it to the fragment
        if (!restore) {
            itemTitle.add(title);
            bundle.putStringArrayList("ITEMTITLE", itemTitle);
            itemListFragment.setArguments(bundle);
            wallet = String.format("%.2f", Double.parseDouble(wallet) - itemCost);
            getBudgetTextView.setText(wallet);
        }

        // constructs fragment and replaces previous one
        FragmentManager fragMag = getFragmentManager();
        FragmentTransaction fragTrans = fragMag.beginTransaction();
        fragTrans.replace(R.id.shoppingScrollView, itemListFragment).commit();
    }

    // Interface used to receive click-response from fragment
    public void deleteRequest(String title) {
        deleteDialog(ShoppingCart.this, title, "Are you sure you want to delete this item?", "YES", "NO", title);
    }

    // Are you sure you want to delete this item
    public AlertDialog deleteDialog(final Activity act, final CharSequence title, CharSequence message, CharSequence buttonYes, CharSequence buttonNo, String itemtitle) {
        flagDeletingItem = true;
        currentTitle = itemtitle;
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(act);
        downloadDialog.setTitle(title);
        downloadDialog.setMessage(message);
        // calls deleteItem if user hits yes
        downloadDialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                flagDeletingItem = false;
                deleteItem(currentTitle);
            }
        });

        downloadDialog.setNegativeButton(buttonNo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                flagDeletingItem = false;
                toastCreator("ITEM NOT DELETED");
            }
        });
        return downloadDialog.show();
    }

    // gets the title from the fragment's table row click-response and deletes it
    public void deleteItem(String title) {
        String item = "";

        // finds the correct item to remove from ArrayList
        for (int i = 0; i < itemTitle.size(); i++) {
            if (itemTitle.get(i).equals(title)) {
                item = itemTitle.get(i);
                itemTitle.remove(i);
                break;
            }
        }

        // adds the price back to user wallet
        double itemCost = Double.parseDouble(item.substring(item.indexOf(":") + 4));
        wallet = String.format("%.2f", Double.parseDouble(wallet) + itemCost);
        getBudgetTextView.setText(wallet);

        // reconstructs fragment and replaces the previous one
        Fragment itemListFragment = new ItemListFragment();
        bundle.putStringArrayList("ITEMTITLE", itemTitle);
        itemListFragment.setArguments(bundle);
        FragmentManager fragMag = getFragmentManager();
        FragmentTransaction fragTrans = fragMag.beginTransaction();
        fragTrans.replace(R.id.shoppingScrollView, itemListFragment).commit();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        savedInstanceState.putString("WALLET", wallet);
        savedInstanceState.putString("SEARCHRESULT", searchResult);
        savedInstanceState.putString("CURRENTTITLE", currentTitle);
        savedInstanceState.putBoolean("ADDINGITEM", flagAddingItem);
        savedInstanceState.putBoolean("SCANSEARCH", flagScanSearch);
        savedInstanceState.putBoolean("DELETINGITEM", flagDeletingItem);
        savedInstanceState.putBundle("BUNDLE", bundle);


        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        flagAddingItem = savedInstanceState.getBoolean("ADDINGITEM");
        flagScanSearch = savedInstanceState.getBoolean("SCANSEARCH");
        flagDeletingItem = savedInstanceState.getBoolean("DELETINGITEM");
        wallet = savedInstanceState.getString("WALLET");
        searchResult = savedInstanceState.getString("SEARCHRESULT");
        currentTitle = savedInstanceState.getString("CURRENTTITLE");
        bundle = savedInstanceState.getBundle("BUNDLE");
        itemTitle = bundle.getStringArrayList("ITEMTITLE");

        getBudgetTextView.setText(wallet);

        // flags are set to true within their methods when they're called
        // they are only set to false after the method is completed
        if (flagScanSearch)
            downloadScanDialog(ShoppingCart.this, "No Scanner Installed", "Download a Bar Code Scanner?", "Yes", "No").show();

        if (flagAddingItem) {
            int nameEnd = searchResult.indexOf("Price");
            String itemName = searchResult.substring(10, nameEnd);
            Double itemCost = Double.parseDouble(searchResult.substring(16 + itemName.length()));
            String walletPreview = String.format("%.2f", Double.parseDouble(wallet) - itemCost);
            addDialog(ShoppingCart.this, itemName, "DO YOU WANT TO ADD THIS ITEM?" + "\n MONEY LEFT IN WALLET AFTER PURCHASE: " + walletPreview, "YES", "NO", itemName, itemCost).show();
        }

        if (flagDeletingItem) {
            deleteDialog(ShoppingCart.this, currentTitle, "Are you sure you want to delete this item?", "YES", "NO", currentTitle);
        }

        Fragment itemListFragment = new ItemListFragment();
        itemListFragment.setArguments(bundle);
        FragmentManager fragMag = getFragmentManager();
        FragmentTransaction fragTrans = fragMag.beginTransaction();
        fragTrans.replace(R.id.shoppingScrollView, itemListFragment).commit();

    }


}