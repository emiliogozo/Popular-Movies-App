package com.movie.flickster.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.movie.flickster.Utility;

import java.util.Calendar;

/**
 * Provider for movie data.
 */
public class MovieProvider extends ContentProvider {
    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private MovieDbHelper mOpenHelper;

    static final int MOVIE = 100;
    static final int MOVIE_WITH_DATE = 110;
    static final int MOVIE_WITH_FILTER = 120;
    static final int MOVIE_WITH_DATE_AND_ID = 111;

    //movie.date = ?
    private static final String sDateSelection =
            MovieContract.MovieEntry.TABLE_NAME +
                    "." + MovieContract.MovieEntry.COLUMN_DATE + " = ? ";

    private static final String sPopularSelection =
            MovieContract.MovieEntry.TABLE_NAME +
                    "." + MovieContract.MovieEntry.COLUMN_DATE + " = ? AND " +
                    MovieContract.MovieEntry.TABLE_NAME +
                    "." + MovieContract.MovieEntry.COLUMN_POPULAR + " = ? ";

    private static final String sTopRatedSelection =
            MovieContract.MovieEntry.TABLE_NAME +
                    "." + MovieContract.MovieEntry.COLUMN_DATE + " = ? AND " +
                    MovieContract.MovieEntry.TABLE_NAME +
                    "." + MovieContract.MovieEntry.COLUMN_TOP_RATED + " = ? ";

    private static final String sDateWithIdSelection =
            MovieContract.MovieEntry.TABLE_NAME +
                    "." + MovieContract.MovieEntry.COLUMN_DATE + " = ? AND " +
                    MovieContract.MovieEntry.TABLE_NAME +
                    "." + MovieContract.MovieEntry._ID + " = ? ";

    @Override
    public boolean onCreate() {
        mOpenHelper = new MovieDbHelper(getContext());
        return false;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case MOVIE:
                return MovieContract.MovieEntry.CONTENT_TYPE;
            case MOVIE_WITH_DATE:
                return MovieContract.MovieEntry.CONTENT_TYPE;
            case MOVIE_WITH_FILTER:
                return MovieContract.MovieEntry.CONTENT_TYPE;
            case MOVIE_WITH_DATE_AND_ID:
                return MovieContract.MovieEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor retCursor;

        switch (sUriMatcher.match(uri)) {
            case MOVIE: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        MovieContract.MovieEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case MOVIE_WITH_DATE: {
                long date = MovieContract.MovieEntry.getDateFromUri(uri);
                retCursor = mOpenHelper.getReadableDatabase().query(
                        MovieContract.MovieEntry.TABLE_NAME,
                        projection,
                        sDateSelection,
                        new String[]{Long.toString(date)},
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case MOVIE_WITH_FILTER: {
                long date = Utility.nearestDay(Calendar.getInstance().getTime());
                String filter = MovieContract.MovieEntry.getFilterFromUri(uri);
                switch (filter) {
                    case MovieContract.MovieEntry.FILTER_POPULAR: {
                        retCursor = mOpenHelper.getReadableDatabase().query(
                                MovieContract.MovieEntry.TABLE_NAME,
                                projection,
                                sPopularSelection,
                                new String[]{Long.toString(date), "1"},
                                null,
                                null,
                                sortOrder != null ? sortOrder : MovieContract.MovieEntry.COLUMN_POPULARITY + " DESC"
                        );
                        break;
                    }
                    case MovieContract.MovieEntry.FILTER_TOP_RATED: {
                        retCursor = mOpenHelper.getReadableDatabase().query(
                                MovieContract.MovieEntry.TABLE_NAME,
                                projection,
                                sTopRatedSelection,
                                new String[]{Long.toString(date), "1"},
                                null,
                                null,
                                sortOrder != null ? sortOrder : MovieContract.MovieEntry.COLUMN_USER_RATING + " DESC"
                        );
                        break;
                    }
                    default: {
                        retCursor = mOpenHelper.getReadableDatabase().query(
                                MovieContract.MovieEntry.TABLE_NAME,
                                projection,
                                sPopularSelection,
                                new String[]{Long.toString(date), "1"},
                                null,
                                null,
                                sortOrder
                        );
                    }
                }


                break;
            }
            case MOVIE_WITH_DATE_AND_ID: {
                long date = MovieContract.MovieEntry.getDateFromUri(uri);
                long id = MovieContract.MovieEntry.getIdFromUri(uri);
                retCursor = mOpenHelper.getReadableDatabase().query(
                        MovieContract.MovieEntry.TABLE_NAME,
                        projection,
                        sDateWithIdSelection,
                        new String[]{Long.toString(date), Long.toString(id)},
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case MOVIE: {
                long _id = db.insertWithOnConflict(MovieContract.MovieEntry.TABLE_NAME,
                        null, values, SQLiteDatabase.CONFLICT_REPLACE);
                if ( _id > 0 )
                    returnUri = MovieContract.MovieEntry.buildMovieUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case MOVIE:
                rowsUpdated = db.update(MovieContract.MovieEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);
        int rowsDeleted;

        // this makes delete all rows return the number of rows deleted
        if ( null == selection ) selection = "1";

        switch (match) {
            case MOVIE: {
                rowsDeleted = db.delete(
                        MovieContract.MovieEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] valuesArr) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case MOVIE:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues values : valuesArr) {
                        long _id = db.insertWithOnConflict(MovieContract.MovieEntry.TABLE_NAME,
                                null, values, SQLiteDatabase.CONFLICT_REPLACE);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, valuesArr);
        }
    }

    static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = MovieContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, MovieContract.PATH_MOVIE, MOVIE);
        matcher.addURI(authority, MovieContract.PATH_MOVIE + "/#", MOVIE_WITH_DATE);
        matcher.addURI(authority, MovieContract.PATH_MOVIE + "/*", MOVIE_WITH_FILTER);
        matcher.addURI(authority, MovieContract.PATH_MOVIE + "/#/#", MOVIE_WITH_DATE_AND_ID);

        return matcher;
    }
}
