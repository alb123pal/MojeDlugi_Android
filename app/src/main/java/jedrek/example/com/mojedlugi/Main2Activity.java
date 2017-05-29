package jedrek.example.com.mojedlugi;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.provider.ContactsContract;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class Main2Activity extends AppCompatActivity {
    ExpandableListView expandableListView;
    ExpandableListAdapter expandableListAdapter;
    List<String> expandableListTitle;
    HashMap<String, List<String>> expandableListDetail;
    String loggedUserEmail;
    Context context;
    DataSnapshot dlugiDataSnapshot;
    DataSnapshot usersDataSnapshot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        context = this;

        loggedUserEmail = getIntent().getStringExtra("loggedUserEmail");

        expandableListView = (ExpandableListView) findViewById(R.id.expandableListView);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference();

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("--", "dostep do bazy danych w " + dataSnapshot.getKey());
                for(DataSnapshot child : dataSnapshot.getChildren()) {
                    Log.d("--", "child: " + child.getKey());
                }
                dlugiDataSnapshot = dataSnapshot.child("dlugi");
                usersDataSnapshot = dataSnapshot.child("users");

                expandableListDetail = ExpandableListDataPump.getData(dlugiDataSnapshot, usersDataSnapshot, loggedUserEmail);
                expandableListTitle = new ArrayList<>(expandableListDetail.keySet());
                expandableListAdapter = new CustomExpandableListAdapter(context, expandableListTitle, expandableListDetail);
                expandableListView.setAdapter(expandableListAdapter);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                Notification n = builder.setContentText("Twoje dlugi zostaly zaktualizowane").setContentTitle("MojeDlugi").setSmallIcon(R.drawable.add).build();
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(5656, n);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("--", "Failed to read value.");
            }
        });

        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int groupPosition, int childPosition, long id) {
                String str = expandableListAdapter.getGroup(groupPosition).toString();
                str = expandableListDetail.get(str).get(childPosition);
                String opis = str.split(";")[2];
                String data = str.split(";")[3];
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Opis: " + opis + "\n" + "Data: " + data);
                builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                builder.create().show();

                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_add) {
            Intent i = new Intent(this, Main3Activity.class);
            i.putExtra("loggedUserEmail", loggedUserEmail);
            startActivity(i);
        }
        return true;
    }
}

class ExpandableListDataPump {
    public static HashMap<String, List<String>> getData(DataSnapshot dlugiDataSnapshot, DataSnapshot usersDataSnapshot, final String loggedUserEmail) {
        final HashMap<String, List<String>> expandableListData = new HashMap<>();

        for (DataSnapshot user : usersDataSnapshot.getChildren()){
            if(user.child("email").getValue(String.class).equals(loggedUserEmail)) {
                for(DataSnapshot friend : user.child("friends").getChildren()) {
                    expandableListData.put(friend.getKey(), new ArrayList<String>());
                    Log.d("--", "Dodano frienda " + friend.getKey());
                }
            }
        }

        for(DataSnapshot dlug : dlugiDataSnapshot.getChildren()) {
            if(!dlug.child("do").getValue(String.class).equals(loggedUserEmail) && !dlug.child("od").getValue(String.class).equals(loggedUserEmail)) {
                continue;
            }
            String str;
            String kwota = "";
            String kategoria = "";
            String opis = "";
            String data = "";
            for(DataSnapshot val : dlug.getChildren()) {
                if(val.getKey().equals("kwota")) {
                    if(dlug.child("od").getValue(String.class).equals(loggedUserEmail)) {
                        Log.d("--", "Ustawiam kwote na -");
                        kwota = (- val.getValue(Double.class)) + "";
                    } else if(dlug.child("do").getValue(String.class).equals(loggedUserEmail)) {
                        Log.d("--", "Ustawiam kwote na +");
                        kwota = val.getValue(Double.class) + "";
                    }
                } else if(val.getKey().equals("kategoria")) {
                    kategoria = val.getValue(String.class);
                } else if(val.getKey().equals("opis")) {
                    opis = val.getValue(String.class);
                } else if(val.getKey().equals("data")) {
                    data = val.getValue(String.class);
                }
            }
            str = kwota + ";" + kategoria + ";" + opis + ";" + data;
            Log.d("--", str);
            String username = "";
            String userEmail = "";
            if(dlug.child("od").getValue(String.class).equals(loggedUserEmail)) {
                userEmail = dlug.child("do").getValue(String.class);
            } else if(dlug.child("do").getValue(String.class).equals(loggedUserEmail)) {
                userEmail = dlug.child("od").getValue(String.class);
            }

            outerloop:
            for(DataSnapshot user : usersDataSnapshot.getChildren()) {
                for(DataSnapshot child : user.getChildren()) {
                    if(child.getKey().equals("email")) {
                        if(child.getValue(String.class).equals(userEmail)) {
                            username = user.getKey();
                            break outerloop;
                        }
                    }
                }
            }

            if(!expandableListData.containsKey(username)) {
                expandableListData.put(username, new ArrayList<String>());
            }

            expandableListData.get(username).add(str);
        }
        return  expandableListData;
    }
}

class CustomExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<String> expandableListTitle;
    private HashMap<String, List<String>> expandableListDetail;

    public CustomExpandableListAdapter(Context context, List<String> expandableListTitle,
                                       HashMap<String, List<String>> expandableListDetail) {
        this.context = context;
        this.expandableListTitle = expandableListTitle;
        this.expandableListDetail = expandableListDetail;
    }

    @Override
    public Object getChild(int listPosition, int expandedListPosition) {
        String str = this.expandableListDetail.get(this.expandableListTitle.get(listPosition))
                .get(expandedListPosition);
        String cat = str.split(";")[1];

        return cat;
    }

    public double getChildValue(int listPosition, int expandedListPosition) {
        String str = this.expandableListDetail.get(this.expandableListTitle.get(listPosition))
                .get(expandedListPosition);

        return Double.parseDouble(str.split(";")[0]);
    }

    @Override
    public long getChildId(int listPosition, int expandedListPosition) {
        return expandedListPosition;
    }

    @Override
    public View getChildView(int listPosition, final int expandedListPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        final String expandedListText = (String) getChild(listPosition, expandedListPosition);
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.list_item, null);
        }
        TextView expandedListTextView = (TextView) convertView
                .findViewById(R.id.expandedListItem);
        expandedListTextView.setText(expandedListText);

        TextView expandedTextValue = (TextView) convertView.findViewById(R.id.expandedListItemBalance);
        double val = getChildValue(listPosition, expandedListPosition);
        expandedTextValue.setText("" + val);

        return convertView;
    }

    @Override
    public int getChildrenCount(int listPosition) {
        return this.expandableListDetail.get(this.expandableListTitle.get(listPosition))
                .size();
    }

    @Override
    public Object getGroup(int listPosition) {
        return this.expandableListTitle.get(listPosition);
    }

    @Override
    public int getGroupCount() {
        return this.expandableListTitle.size();
    }

    @Override
    public long getGroupId(int listPosition) {
        return listPosition;
    }

    @Override
    public View getGroupView(int listPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String listTitle = (String) getGroup(listPosition);
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.list_group, null);
        }
        TextView listTitleTextView = (TextView) convertView
                .findViewById(R.id.listTitle);
        listTitleTextView.setTypeface(null, Typeface.BOLD);
        listTitleTextView.setText(listTitle);

        double balance = 0;
        for (String dlug : expandableListDetail.get(listTitle)) {
            balance += Double.parseDouble(dlug.split(";")[0]);
        }
        TextView listTitleBalance = (TextView) convertView.findViewById(R.id.listTitleBalance);
        listTitleBalance.setText(String.format(Locale.US, "%.2f", balance));
        if(balance < 0) {
            listTitleBalance.setTextColor(Color.RED);
        } else if(balance > 0) {
            listTitleBalance.setTextColor(Color.GREEN);
        } else if(balance == 0) {
            listTitleBalance.setTextColor(Color.GRAY);
        }
        listTitleBalance.setTypeface(null, Typeface.BOLD);

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int listPosition, int expandedListPosition) {
        return true;
    }
}