package jedrek.example.com.mojedlugi;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {
    private GoogleApiClient mGoogleApiClient;
    String loggedUserEmail = "";
    Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestServerAuthCode("727501306188-597tuen37rl46njqt5h7a6e3m0od2gvm.apps.googleusercontent.com")
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        findViewById(R.id.signInButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signInButtonOnClick(view);
            }
        });

        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            Log.d("--", "Got cached sign-in");
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently.  Cross-device
            // single sign-on will occur in this branch.
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {
                    handleSignInResult(googleSignInResult);
                }
            });
        }
    }

    public void signInButtonOnClick(View view) {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, 1234);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1234) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d("--", "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            Log.d("--", "Zalogowano użytkownika: " + acct.getEmail());
            loggedUserEmail = acct.getEmail();

            DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("users");
            myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for(DataSnapshot user : dataSnapshot.getChildren()) {
                        if(user.child("email").getValue(String.class).equals(loggedUserEmail)) {
                            Log.d("--", "Znaleziono uzytkownika w bazie danych.");
                            TextView welcomeText = (TextView) findViewById(R.id.welcome_text);
                            welcomeText.setText(getString(R.string.welcome) + " \n" + loggedUserEmail);
                            welcomeText.setVisibility(View.VISIBLE);
                            findViewById(R.id.signInButton).setVisibility(View.INVISIBLE);
                            findViewById(R.id.logOutButton).setVisibility(View.VISIBLE);
                            findViewById(R.id.nextButton).setVisibility(View.VISIBLE);
                            return;
                        }
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Nowy użytkownik");
                    builder.setMessage("Wybierz swoją nazwę użytkownika:");
                    final EditText input = new EditText(context);
                    builder.setView(input);
                    builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            addUser(input.getText().toString());
                        }
                    });
                    builder.create().show();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
        }
    }

    private void addUser(final String userName) {
        final DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("users");
        final boolean[] addedSuccessfully = {false};
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot user : dataSnapshot.getChildren()) {
                    if(user.getKey().equals(userName)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setMessage("Taki użytkownik już istnieje!");
                        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        });
                        builder.create().show();
                        return;
                    }

                }
                FirebaseDatabase.getInstance().getReference("users/" + userName + "/email").setValue(loggedUserEmail);
                TextView welcomeText = (TextView) findViewById(R.id.welcome_text);
                welcomeText.setText(getString(R.string.welcome) + " \n" + loggedUserEmail);
                welcomeText.setVisibility(View.VISIBLE);
                findViewById(R.id.signInButton).setVisibility(View.INVISIBLE);
                findViewById(R.id.logOutButton).setVisibility(View.VISIBLE);
                findViewById(R.id.nextButton).setVisibility(View.VISIBLE);
                addedSuccessfully[0] = true;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("--", "Connection failed.");
    }

    public void logOutButtonOnClick(View view) {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        Log.d("--", "Wylogowano użytkownika.");

                        findViewById(R.id.welcome_text).setVisibility(View.INVISIBLE);
                        findViewById(R.id.signInButton).setVisibility(View.VISIBLE);
                        findViewById(R.id.logOutButton).setVisibility(View.INVISIBLE);
                        findViewById(R.id.nextButton).setVisibility(View.INVISIBLE);
                    }
                });
    }

    public void nextButtonOnClick(View view) {
        Intent i = new Intent(this, Main2Activity.class);
        i.putExtra("loggedUserEmail", loggedUserEmail);
        startActivity(i);
    }
}

