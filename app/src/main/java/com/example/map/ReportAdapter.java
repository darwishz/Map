package com.example.map;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    private List<Report> reports;
    private Context context;

    public ReportAdapter(Context context, List<Report> reports) {
        this.context = context;
        this.reports = reports;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.cardview_archive_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Report report = reports.get(position);

        TextView timestampTextView = holder.timestampTextView;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedTimestamp = sdf.format(report.getTimestamp().toDate());
        timestampTextView.setText(formattedTimestamp);
        TextView descTextView = holder.descTextView;
        descTextView.setText(report.getDesc());
        // Format and display location
        TextView locationTextView = holder.locationTextView;
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault()); // Use device's locale
            List<Address> addresses = geocoder.getFromLocation(report.getLocation().getLatitude(), report.getLocation().getLongitude(), 1);
            if (addresses.size() > 0) {
                Address address = addresses.get(0);
                String locationName = address.getLocality() + ", " + address.getCountryName();
                locationTextView.setText(locationName);
            } else {
                Log.d("ReportAdapter", "Geocoder returned no addresses"); // Log for debugging
                locationTextView.setText("Unknown Location");
            }
        } catch (IOException e) {
            Log.e("ReportAdapter", "Error retrieving location: " + e.getMessage()); // Log for debugging
            locationTextView.setText("Error retrieving location");
        }
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView timestampTextView, locationTextView, descTextView, userIdTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            timestampTextView = itemView.findViewById(R.id.TVReportTime);
            locationTextView = itemView.findViewById(R.id.TVReportLocation);
            descTextView = itemView.findViewById(R.id.TVReportTitle);
//            userIdTextView = itemView.findViewById(R.id.userIdTextView);
//            TextView textViewReportTitle,textViewReportDate,textViewReportTime,textViewReportLocation;
//            textViewReportTitle = itemView.findViewById(R.id.TVReportTitle);
//            textViewReportDate = itemView.findViewById(R.id.TVReportDate);
//            textViewReportTime = itemView.findViewById(R.id.TVReportTime);
//            textViewReportLocation = itemView.findViewById(R.id.TVReportLocation);
        }
    }
}

