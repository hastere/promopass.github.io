package nmotion.promopass;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ListSavedAds extends AppCompatActivity {

    private Menu optionsMenu;
    private Toolbar toolbar;
    private int MENU_CLEAR = Menu.FIRST;
    private int MENU_BLOCK = Menu.FIRST + 1;
    private int MENU_FAVORITE = Menu.FIRST + 2;
    private int MENU_CLOSE = Menu.FIRST + 3;
    private boolean isSelected;
    private ListView savedAdsView;
    private PromoPassAdapter savedAds;
    private int selectedPosition;
    private View selectedView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_saved_ads);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        savedAdsView = (ListView) findViewById(R.id.savedAds_list);
        List<ReceivedAd> list = new ArrayList<ReceivedAd>();
        savedAds = new PromoPassAdapter(this);
        savedAdsView.setAdapter(savedAds);

        // get all saved ads for consumer listed by received date

        String consumerID = null;
        if(InternetCheck.isNetworkConnected(this)){

            consumerID = DeviceIdentifier.id(this);

            JSONArray savedReceivedAds = Reader.getResults("http://fendatr.com/api/v1/received/ad/"+consumerID+"/saved");

            try {
                JSONObject jsonTemp;

                for (int i = 0; i < savedReceivedAds.length(); i++) {
                    jsonTemp = savedReceivedAds.getJSONObject(i);
                    String businessID = jsonTemp.getString("BusinessID");
                    String adID = jsonTemp.getString("AdID");

                    JSONArray businessName = Reader.getResults("http://fendatr.com/api/v1/business/" + businessID + "/name");
                    JSONArray adTitle = Reader.getResults("http://fendatr.com/api/v1/ad/" + adID);

                    JSONObject name = businessName.getJSONObject(0);
                    JSONObject title = adTitle.getJSONObject(0);

                    ReceivedAd receivedAd = new ReceivedAd(jsonTemp.getString("ReceivedAdID"),
                            adID,
                            businessID,
                            name.getString("Name"),
                            consumerID,
                            title.getString("Title"));
                    savedAds.add(receivedAd);

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        final String finalConsumerID = consumerID;
        savedAdsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                removeSelection();
                Intent intent = new Intent(view.getContext(), ViewAd.class);

                intent.putExtra("ReceivedAdID", savedAds.getItem(position).getReceivedAdID());
                intent.putExtra("AdID", savedAds.getItem(position).getAdID());
                intent.putExtra("BusinessID", savedAds.getItem(position).getBusinessID());
                intent.putExtra("BusinessName", savedAds.getItem(position).toString());
                intent.putExtra("ConsumerID", finalConsumerID);
                intent.putExtra("IsSavedAd", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });

        savedAdsView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (selectedView != null)
                    selectedView.setBackgroundColor(ContextCompat.getColor(selectedView.getContext(), android.R.color.transparent));
                selectedPosition = position;
                selectedView = view;
                view.setBackgroundColor(Color.GRAY);

                if (!isSelected) {
                    toolbar.setBackgroundColor(ContextCompat.getColor(toolbar.getContext(), R.color.colorPrimarySelected));
                    optionsMenu.add(0, R.id.action_favorite, MENU_FAVORITE, R.string.action_favorite)
                            .setIcon(R.drawable.favorite)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    optionsMenu.add(0, R.id.action_block, MENU_BLOCK, R.string.action_block)
                            .setIcon(R.drawable.block)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    optionsMenu.add(0, R.id.action_close, MENU_CLOSE, R.string.action_close)
                            .setIcon(R.drawable.close)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    optionsMenu.add(0, R.id.action_clear, MENU_CLEAR, R.string.action_clear)
                            .setIcon(R.drawable.clear)
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    isSelected = true;
                }
                return true;
            }
        });

        savedAdsView.setEmptyView(findViewById(R.id.emptySaved));

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK ) {
            if(isSelected) {
                removeSelection();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        optionsMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if(InternetCheck.isNetworkConnected(this)){

            ReceivedAd selectedAd = savedAds.getItem(selectedPosition);
            switch (item.getItemId()) {
                case R.id.action_favorite:
                    Reader.update("http://fendatr.com/api/v1/preferences/consumer/" + selectedAd.getConsumerID() + "/business/" + selectedAd.getBusinessID() +  "/favorite");
                    Toast.makeText(this, selectedAd.toString() + getString(R.string.favorite_string), Toast.LENGTH_LONG).show();
                    break;
                case R.id.action_block:
                    Reader.update("http://fendatr.com/api/v1/preferences/consumer/" + selectedAd.getConsumerID() + "/business/" + selectedAd.getBusinessID() + "/block");
                    Toast.makeText(this, selectedAd.toString() + getString(R.string.block_string), Toast.LENGTH_LONG).show();
                    String businessID = selectedAd.getBusinessID();
                    for(int i=0; i<savedAds.getCount(); i++){
                        ReceivedAd currentAd = savedAds.getItem(i);
                        if(businessID.equals(currentAd.getBusinessID())){
                            savedAds.remove(currentAd);
                            i--;
                        }
                    }
                    break;
                case R.id.action_clear:
                    Reader.update("http://fendatr.com/api/v1/received/ad/" + selectedAd.getReceivedAdID() + "/clear");
                    Toast.makeText(this, selectedAd.getTitle() + getString(R.string.clear_string),
                            Toast.LENGTH_LONG).show();
                    savedAds.remove(selectedAd);
                    break;
            }

            removeSelection();

        }

        return false;
    }

    private void removeSelection(){


        toolbar.setBackgroundColor(ContextCompat.getColor(toolbar.getContext(), R.color.colorPrimary));
        optionsMenu.removeItem(R.id.action_close);
        optionsMenu.removeItem(R.id.action_clear);
        optionsMenu.removeItem(R.id.action_favorite);
        optionsMenu.removeItem(R.id.action_block);

        if(selectedView != null)
            selectedView.setBackgroundColor(ContextCompat.getColor(selectedView.getContext(), android.R.color.transparent));
        selectedPosition = -1;
        isSelected=false;
    }

}
