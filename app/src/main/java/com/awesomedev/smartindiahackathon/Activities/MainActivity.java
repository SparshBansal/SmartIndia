package com.awesomedev.smartindiahackathon.Activities;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.awesomedev.smartindiahackathon.Activities.DetailsActivity;
import com.awesomedev.smartindiahackathon.Data.DatabaseContract;
import com.awesomedev.smartindiahackathon.Models.Counter;
import com.awesomedev.smartindiahackathon.Models.FlightDetails;
import com.awesomedev.smartindiahackathon.R;
import com.awesomedev.smartindiahackathon.Util.Utilities;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;

import static com.awesomedev.smartindiahackathon.Data.DatabaseContract.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = MainActivity.class.getSimpleName();


    @BindView(R.id.b_estimate)
    Button bEstimate;

    @BindView(R.id.sv_airport)
    Spinner svAirport;

    @BindView(R.id.sv_carrier)
    Spinner svCarrier;

    @BindView(R.id.sv_flight)
    Spinner svFlight;

    @BindString(R.string.KEY_AIRPORT)
    String KEY_AIRPORT;

    @BindString(R.string.KEY_CARRIER)
    String KEY_CARRIER;

    @BindString(R.string.KEY_FLIGHT)
    String KEY_FLIGHT;



    // Database Reference for accessing firebase application
    private static DatabaseReference reference = null;
    private static FirebaseDatabase firebaseDatabase = null;

    // Loader constants
    private static final int AIRPORT_LOADER_ID = 100;
    private static final int CARRIER_LOADER_ID = 101;
    private static final int FLIGHT_LOADER_ID = 102;

    private static final String KEY_AIRPORT_ID = "AIRPORT_ID";
    private static final String KEY_CARRIER_ID = "CARRIER_ID";


    private Cursor airportCursor = null;
    private Cursor carrierCursor = null;
    private Cursor flightCursor = null;

    CursorAdapter airportAdapter, carrierAdapter, flightAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the action bar
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        setContentView(R.layout.activity_main);


        // Bind all the views
        ButterKnife.bind(this);

        bEstimate.setOnClickListener(this);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        firebaseDatabase = FirebaseDatabase.getInstance();
        reference = firebaseDatabase.getReference();

        airportAdapter = new SimpleCursorAdapter(MainActivity.this,
                R.layout.airport_list_item,
                airportCursor,
                new String[]{AirportEntry.COLUMN_AIRPORT_NAME},
                new int[]{R.id.tv_airport},
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        );

        carrierAdapter = new SimpleCursorAdapter(MainActivity.this,
                R.layout.airport_list_item,
                carrierCursor,
                new String[]{CarrierEntry.COLUMN_CARRIER_NAME},
                new int[]{R.id.tv_airport},
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        );


        flightAdapter = new SimpleCursorAdapter(MainActivity.this,
                R.layout.airport_list_item,
                flightCursor,
                new String[]{FlightEntry.COLUMN_FLIGHT_NUMBER},
                new int[]{R.id.tv_airport},
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        );


        svAirport.setAdapter(airportAdapter);
        svCarrier.setAdapter(carrierAdapter);
        svFlight.setAdapter(flightAdapter);


        svAirport.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Cursor mCursor = airportAdapter.getCursor();
                mCursor.moveToPosition(position);
                final int airport_id = mCursor.getInt(mCursor.getColumnIndex(AirportEntry._ID));

                // Now kick of the carrier loader
                Bundle args = new Bundle();
                args.putInt(KEY_AIRPORT_ID, airport_id);
                getSupportLoaderManager().restartLoader(CARRIER_LOADER_ID, args, MainActivity.this);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        svCarrier.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Cursor mCursor = carrierAdapter.getCursor();
                mCursor.moveToPosition(position);
                final int carrier_id = mCursor.getInt(mCursor.getColumnIndex(CarrierEntry._ID));

                // Now kick off the flight loader
                Bundle args = new Bundle();
                args.putInt(KEY_CARRIER_ID, carrier_id);
                getSupportLoaderManager().restartLoader(FLIGHT_LOADER_ID, args, MainActivity.this);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        getSupportLoaderManager().restartLoader(AIRPORT_LOADER_ID, null, this);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot airportSnapshot : dataSnapshot.getChildren()) {

                    final String airportName = airportSnapshot.getKey();

                    // Check if the airport is already in the database
                    int airport_id = Utilities.getAirportId(MainActivity.this, airportName);

                    // If not insert in the database
                    if (airport_id == -1) {

                        ContentValues values = new ContentValues();
                        values.put(AirportEntry.COLUMN_AIRPORT_NAME, airportName);

                        Uri returnUri = getContentResolver().insert(AirportEntry.CONTENT_URI, values);
                        airport_id = (int) ContentUris.parseId(returnUri);
                        Log.d(TAG, "onDataChange: Airport Id : " + airport_id);
                    }

                    for (DataSnapshot carrierSnapshot : airportSnapshot.getChildren()) {

                        // Check if carrier is in the database
                        final String carrierName = carrierSnapshot.getKey();

                        int carrier_id = Utilities.getCarrierId(MainActivity.this, airport_id, carrierName);
                        if (carrierName.equals("Jet Airways") && airportName.equals("NSIT")){
                            Log.d(TAG, "onDataChange: NSIT,JET AIRWAYS : " + carrier_id);
                        }
                        // If not insert in the database
                        if (carrier_id == -1) {
                            ContentValues values = new ContentValues();
                            values.put(CarrierEntry.COLUMN_AIRPORT_KEY, airport_id);
                            values.put(CarrierEntry.COLUMN_CARRIER_NAME, carrierName);

                            Uri returnUri = getContentResolver().insert(CarrierEntry.CONTENT_URI, values);
                            carrier_id = (int) ContentUris.parseId(returnUri);
                            if (carrierName.equals("Jet Airways") && airportName.equals("NSIT")){
                                Log.d(TAG, "onDataChange: NSIT,JET AIRWAYS , INSERT: " + carrier_id);
                            }
                        }

                        for (DataSnapshot mSnapshot : carrierSnapshot.getChildren()) {
                            if (mSnapshot.getKey().equals("flight")) {
                                List<FlightDetails> flights = new ArrayList<FlightDetails>();
                                for (DataSnapshot flight : mSnapshot.getChildren()) {
                                    FlightDetails flightDetails = flight.getValue(FlightDetails.class);
                                    flights.add(flightDetails);
                                    Log.d(TAG, "onDataChange: " + flightDetails.getFlightNo());
                                }
                                for (FlightDetails flight : flights) {
                                    if (flight.getFlightNo().equals("IX-202")){
                                        Log.d(TAG, "onDataChange: flight IX-202 , Carrier : " + carrierName + " airport : " + airportName);
                                    }
                                    ContentValues values = new ContentValues();

                                    values.put(FlightEntry.COLUMN_FLIGHT_NUMBER, flight.getFlightNo());
                                    values.put(FlightEntry.COLUMN_CARRIER_KEY, carrier_id);
                                    values.put(FlightEntry.COLUMN_DELAYED, flight.getDelayed());
                                    values.put(FlightEntry.COLUMN_DEPARTURE_TIME, flight.getDepartureTime());
                                    values.put(FlightEntry.COLUMN_SOURCE, flight.getSource());
                                    values.put(FlightEntry.COLUMN_DESTINATION, flight.getDestination());


                                    final Uri uri = getContentResolver().insert(FlightEntry.CONTENT_URI, values);
                                    final int _id = (int) ContentUris.parseId(uri);

                                    if (carrierName.equals("Jet Airways") && airportName.equals("NSIT")){
                                        Log.d(TAG, "onDataChange: NSIT,JET AIRWAYS , INSERT FLIGHT: " + _id);

                                    }
                                }
                            }
                            if (mSnapshot.getKey().equals("carrier")) {
                                List<Counter> counters = new ArrayList<Counter>();
                                for (DataSnapshot counterSnapshot : mSnapshot.getChildren()) {
                                    counters.add(counterSnapshot.getValue(Counter.class));
                                }
                                for (Counter counter : counters) {
                                    ContentValues values = new ContentValues();
                                    values.put(CounterEntry.COLUMN_CARRIER_KEY, carrier_id);
                                    values.put(CounterEntry.COLUMN_COUNTER_COUNT, counter.getCounterCount());
                                    values.put(CounterEntry.COLUMN_COUNTER_NUMBER, counter.getCounterNumber());
                                    values.put(CounterEntry.COLUMN_COUNTER_THROUGHPUT, counter.getThroughput());
                                    values.put(CounterEntry.COLUMN_COUNTER_AVG_WAITING_TIME, counter.getAvgWaitingTime());

                                    getContentResolver().insert(CounterEntry.CONTENT_URI, values);
                                }
                            }
                        }
                    }
                }
                getSupportLoaderManager().restartLoader(AIRPORT_LOADER_ID, null, MainActivity.this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.b_estimate:

                Cursor airportCursor = airportAdapter.getCursor();
                Cursor carrierCursor = carrierAdapter.getCursor();
                Cursor flightCursor = flightAdapter.getCursor();

                airportCursor.moveToPosition(svAirport.getSelectedItemPosition());
                carrierCursor.moveToPosition(svCarrier.getSelectedItemPosition());
                flightCursor.moveToPosition(svFlight.getSelectedItemPosition());

                final String airport = airportCursor.getString(airportCursor.getColumnIndex(AirportEntry.COLUMN_AIRPORT_NAME));
                final String carrier = carrierCursor.getString(carrierCursor.getColumnIndex(CarrierEntry.COLUMN_CARRIER_NAME));
                final String flightNumber = flightCursor.getString(flightCursor.getColumnIndex(FlightEntry.COLUMN_FLIGHT_NUMBER));

                final int airport_id = airportCursor.getInt(airportCursor.getColumnIndex(AirportEntry._ID));
                final int carrier_id = carrierCursor.getInt(carrierCursor.getColumnIndex(CarrierEntry._ID));

                Log.d(TAG, "onClick: airport : " + airport);
                Log.d(TAG, "onClick: carrier : " + carrier);
                Log.d(TAG, "onClick: flight : " + flightNumber);

                if (airport.equals("") || carrier.equals("") || flightNumber.equals("")) {
                    Toast.makeText(this, "None of the fields should be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.d(TAG, "onClick: Carrier Id : " + Integer.toString(carrier_id));

                Intent intent = new Intent(this, DetailsActivity.class);
                intent = intent.putExtra(KEY_AIRPORT, airport)
                        .putExtra(KEY_CARRIER, carrier)
                        .putExtra(KEY_FLIGHT, flightNumber)
                        .putExtra(KEY_AIRPORT_ID, airport_id)
                        .putExtra(KEY_CARRIER_ID, carrier_id);
                startActivity(intent);

                break;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == AIRPORT_LOADER_ID) {
            return new CursorLoader(this, AirportEntry.CONTENT_URI,
                    null,
                    null,
                    null,
                    null
            );
        }
        if (id == CARRIER_LOADER_ID) {
            final int airport_id = args.getInt(KEY_AIRPORT_ID);
            Uri uri = CarrierEntry.buildCarrierUri(airport_id);
            return new CursorLoader(this,
                    uri,
                    null,
                    null,
                    null,
                    null
            );
        }

        if (id == FLIGHT_LOADER_ID) {
            final int carrier_id = args.getInt(KEY_CARRIER_ID);
            Uri uri = FlightEntry.getFlightUri(carrier_id);
            return new CursorLoader(this,
                    uri,
                    null,
                    null,
                    null,
                    null
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == AIRPORT_LOADER_ID) {
            Log.d(TAG, "onLoadFinished: " + data.getCount());
            airportAdapter.swapCursor(data);
            airportAdapter.notifyDataSetChanged();
        }
        if (loader.getId() == CARRIER_LOADER_ID) {
            Log.d(TAG, "onLoadFinished: Carrier Data count : " + data.getCount());
            carrierAdapter.swapCursor(data);
            carrierAdapter.notifyDataSetChanged();
        }
        if (loader.getId() == FLIGHT_LOADER_ID) {
            Log.d(TAG, "onLoadFinished: Flight Data Count : " + data.getCount());
            flightAdapter.swapCursor(data);
            flightAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == AIRPORT_LOADER_ID) {
            this.airportCursor = null;
            airportAdapter.notifyDataSetChanged();
        }
    }
}
