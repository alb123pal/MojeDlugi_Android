package jedrek.example.com.mojedlugi;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.provider.ContactsContract;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main3Activity extends AppCompatActivity {
    final Context context = this;
    final List<String> friends = new ArrayList<>();
    String loggedUserEmail = "";
    ArrayAdapter<String> dataAdapter;

    Spinner friendSpinner;
    Spinner kategoriaSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        friendSpinner = (Spinner) findViewById(R.id.friendSpinner);
        kategoriaSpinner = (Spinner) findViewById(R.id.kategoriaSpinner);

        List<String> kategorie = new ArrayList<>();
        kategorie.add("Jedzenie");
        kategorie.add("Rozrywka");
        kategorie.add("Zakupy");
        kategorie.add("Transport");
        kategorie.add("Media");
        kategorie.add("Inne");
        ArrayAdapter<String> kategorieAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, kategorie);
        kategorieAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        kategoriaSpinner.setAdapter(kategorieAdapter);

        loggedUserEmail = getIntent().getStringExtra("loggedUserEmail");
        friends.add("Wybierz znajomego");
        friends.add("...dodaj znajomego");

        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("users");
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot user : dataSnapshot.getChildren()) {
                    if(user.child("email").getValue(String.class).equals(loggedUserEmail)) {
                        for(DataSnapshot friend : user.child("friends").getChildren()) {
                            if(!friends.contains(friend.getKey())) {
                                friends.add(friend.getKey());
                                dataAdapter.notifyDataSetChanged();
                            }
                        }
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, friends);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        friendSpinner.setAdapter(dataAdapter);

        friendSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                if(i == 1) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Dodawanie znajomego");
                    builder.setMessage("Podaj adres e-mail znajomego");
                    builder.setCancelable(true);

                    final EditText input = new EditText(context);
                    //LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams()

                    builder.setView(input);
                    builder.setPositiveButton("dodaj", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            addFriend(input.getText().toString());
                        }
                    });
                    builder.setNegativeButton("anuluj", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();

                    friendSpinner.setSelection(0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        final Calendar myCalendar = Calendar.getInstance();
        final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                // TODO Auto-generated method stub
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                String str = myCalendar.get(Calendar.DAY_OF_MONTH) + ".";
                if(myCalendar.get(Calendar.MONTH) < 10) {
                    str += "0" + myCalendar.get(Calendar.MONTH);
                } else {
                    str += myCalendar.get(Calendar.MONTH);
                }
                str += "." + myCalendar.get(Calendar.YEAR);
                ((TextView) findViewById(R.id.dataTextView)).setText(str);
            }
        };
        findViewById(R.id.dataTextView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DatePickerDialog(context, date, myCalendar.get(Calendar.YEAR),
                        myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
    }

    public void addFriend(final String friendEmail) {
        Log.d("--", "dodaje znajomego " + friendEmail);
        if(friendEmail.equals(loggedUserEmail)) {
            Toast.makeText(context, "Nie możesz dodać siebie do znajomych!", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!friendEmail.endsWith("@gmail.com")) {
            Toast.makeText(context, "Błędny adres e-mail!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseDatabase.getInstance().getReference("users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean friendFound = false;
                String friendName = "";
                HashMap<String, String> friendMap = new HashMap<>();

                for(DataSnapshot user : dataSnapshot.getChildren()) {
                    if(user.child("email").getValue(String.class).equals(friendEmail)) {
                        friendFound = true;
                        friendName = user.getKey();
                    }

                    if(user.child("email").getValue(String.class).equals(loggedUserEmail)) {
                        for(DataSnapshot friend : user.child("friends").getChildren()) {
                            friendMap.put(friend.getKey(), friend.getValue(String.class));
                        }
                    }
                }

                if(friendMap.values().contains(friendEmail)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Ta osoba znajduje się już w twoich znajomych.");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });
                    builder.create().show();

                    return;
                }

                if(!friendFound) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Ta osoba nie korzysta jeszcze z tej aplikacji.");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });
                    builder.create().show();

                    return;
                }

                if(!friends.contains(friendName)) {
                    friends.add(friendName);
                    dataAdapter.notifyDataSetChanged();

                    DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("users");
                    final String finalFriendName = friendName;
                    myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String loggedUserName = "";
                            for(DataSnapshot user : dataSnapshot.getChildren()) {
                                if(user.child("email").getValue(String.class).equals(loggedUserEmail)) {
                                    loggedUserName = user.getKey();
                                }
                            }

                            FirebaseDatabase.getInstance()
                                    .getReference("users/" + loggedUserName + "/friends/" + finalFriendName)
                                    .setValue(friendEmail);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void addDlugOnClick(View view) {
        int item = friendSpinner.getSelectedItemPosition();
        if(item < 2) {
            Toast.makeText(context, "Wybierz znajomego!", Toast.LENGTH_SHORT).show();
            return;
        }

        final String od = friendSpinner.getSelectedItem().toString();
        Log.d("od", od);

        String kwota = ((EditText) findViewById(R.id.kwotaEditText)).getText().toString();

        double kwotaVal = 0;

        try {
            kwotaVal = Double.parseDouble(kwota);
        } catch (NumberFormatException e) {
            Toast.makeText(context, "Podaj odpowiednią kwotę!", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d("kwota", "" + kwotaVal);

        String kategoria = kategoriaSpinner.getSelectedItem().toString();
        Log.d("kategoria", kategoria);

        String data = ((TextView) findViewById(R.id.dataTextView)).getText().toString();
        if(data.contains(";")) {
            data = data.replace(";", "");
        }
        Log.d("data", data);

        String opis = ((EditText) findViewById(R.id.opisEditText)).getText().toString();
        if(opis.contains(";")) {
            opis = opis.replace(";", "");
        }
        Log.d("opis", opis);

        final HashMap<String, Object> dlug = new HashMap<>();
        dlug.put("data", data);
        dlug.put("kategoria", kategoria);
        dlug.put("kwota", kwotaVal);
        dlug.put("opis", opis);
        dlug.put("do", loggedUserEmail);

        FirebaseDatabase.getInstance().getReference("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String odName = "";

                for(DataSnapshot user : dataSnapshot.getChildren()) {
                    if(user.getKey().equals(od)) {
                        odName = user.child("email").getValue(String.class);
                    }
                }

                dlug.put("od", odName);
                DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("dlugi");
                myRef.push().setValue(dlug);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        finish();
    }
}