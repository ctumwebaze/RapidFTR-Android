package com.rapidftr.repository;

import android.content.ContentValues;
import android.database.Cursor;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.rapidftr.database.Database;
import com.rapidftr.database.DatabaseSession;
import com.rapidftr.model.Child;
import com.rapidftr.model.Enquiry;
import com.rapidftr.model.PotentialMatch;
import lombok.Cleanup;
import org.json.JSONException;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.rapidftr.database.Database.PotentialMatchTableColumn.*;

public class PotentialMatchRepository implements Closeable, Repository<PotentialMatch> {

    private final String userName;
    private final DatabaseSession session;

    @Inject
    public PotentialMatchRepository(@Named("USER_NAME") String userName, DatabaseSession session) {
        this.userName = userName;
        this.session = session;
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<PotentialMatch> toBeSynced() throws JSONException {
        return new ArrayList<PotentialMatch>();
    }

    @Override
    public boolean exists(String id) {
        String dbId = (id == null ? "" : id);
        @Cleanup Cursor cursor = session.rawQuery("SELECT * FROM potential_match WHERE id = ?", new String[]{dbId});
        return cursor.moveToNext() && cursor.getCount() > 0;
    }

    @Override
    public PotentialMatch get(String id) throws JSONException {
        @Cleanup Cursor cursor = session.rawQuery("SELECT * FROM potential_match WHERE id = ?", new String[]{id});
        if (cursor.moveToNext()) {
            return buildPotentialMatch(cursor);
        } else {
            return null;
        }
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void createOrUpdate(PotentialMatch potentialMatch) throws JSONException, SQLException {
        createOrUpdateWithoutHistory(potentialMatch);
    }

    @Override
    public HashMap<String, String> getAllIdsAndRevs() throws JSONException {
        return new HashMap<String, String>();
    }

    @Override
    public void createOrUpdateWithoutHistory(PotentialMatch potentialMatch) throws JSONException {
        if (potentialMatch.isDeleted()) {
            delete(potentialMatch);
        } else {
            ContentValues values = new ContentValues();
            values.put(enquiry_id.getColumnName(), potentialMatch.getEnquiryId());
            values.put(child_id.getColumnName(), potentialMatch.getChildId());
            values.put(created_at.getColumnName(), potentialMatch.getCreatedAt());
            values.put(id.getColumnName(), potentialMatch.getUniqueId());
            values.put(revision.getColumnName(), potentialMatch.getRevision());
            values.put(confirmed.getColumnName(), potentialMatch.isConfirmed().toString());

            session.replaceOrThrow(Database.potential_match.getTableName(), null, values);
        }
    }

    public void delete(PotentialMatch potentialMatch) {
        session.execSQL(String.format("DELETE FROM potential_match WHERE id ='%s'", potentialMatch.getUniqueId()));
    }

    @Override
    public List<PotentialMatch> currentUsersUnsyncedRecords() throws JSONException {
        return new ArrayList<PotentialMatch>();
    }

    @Override
    public List<String> getRecordIdsByOwner() throws JSONException {
        return new ArrayList<String>();
    }

    @Override
    public List<PotentialMatch> allCreatedByCurrentUser() throws JSONException {
        return new ArrayList<PotentialMatch>();
    }

    @Override
    public List<PotentialMatch> getRecordsBetween(int previousPageNumber, int pageNumber) {
        return null;
    }

    @Override
    public List<PotentialMatch> getRecordsForFirstPage() throws JSONException {
        return null;
    }

    public List<PotentialMatch> getPotentialMatchesFor(Enquiry enquiry) throws JSONException {
        if (enquiry.getInternalId() == null) {
            return new ArrayList<PotentialMatch>();
        }
        @Cleanup Cursor cursor = session.rawQuery("SELECT * FROM potential_match WHERE enquiry_id = ?", new String[]{enquiry.getInternalId()});
        return buildPotentialMatches(cursor);
    }

    public List<PotentialMatch> getPotentialMatchesFor(Child child) throws JSONException {
        if (child.getInternalId() == null) {
            return new ArrayList<PotentialMatch>();
        }
        @Cleanup Cursor cursor = session.rawQuery("SELECT * FROM potential_match WHERE child_id = ?", new String[]{child.getInternalId()});
        return buildPotentialMatches(cursor);
    }

    private ArrayList<PotentialMatch> buildPotentialMatches(Cursor cursor) {
        ArrayList<PotentialMatch> potentialMatches = new ArrayList<PotentialMatch>();
        while (cursor.moveToNext()) {
            potentialMatches.add(buildPotentialMatch(cursor));
        }
        return potentialMatches;
    }

    private PotentialMatch buildPotentialMatch(Cursor cursor) {
        int enquiryIdIndex = cursor.getColumnIndex(enquiry_id.getColumnName());
        int childIdIndex = cursor.getColumnIndex(child_id.getColumnName());
        int idIndex = cursor.getColumnIndex(id.getColumnName());
        int confirmedIndex = cursor.getColumnIndex(confirmed.getColumnName());
        return new PotentialMatch(cursor.getString(enquiryIdIndex), cursor.getString(childIdIndex), cursor.getString(idIndex),
                Boolean.valueOf(cursor.getString(confirmedIndex)));
    }
}
