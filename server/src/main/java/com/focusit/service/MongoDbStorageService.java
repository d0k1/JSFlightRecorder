package com.focusit.service;

import java.io.InputStream;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import com.focusit.scenario.MongoDbScenario;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;

/**
 * Created by doki on 14.05.16.
 */
@Service
public class MongoDbStorageService
{
    @Inject
    MongoDbFactory mongoDbFactory;
    @Inject
    MappingMongoConverter mappingMongoConverter;

    private GridFsTemplate getGridFsTemplate()
    {
        return new GridFsTemplate(mongoDbFactory, mappingMongoConverter);
    }

    public void storeScreenshot(MongoDbScenario scenario, int position, InputStream stream, boolean error)
    {
        DBObject metaData = new BasicDBObject();
        metaData.put("recordingName", scenario.getRecordingName());
        metaData.put("recordingId", scenario.getRecordingId());
        metaData.put("experimentId", scenario.getExperimentId());
        metaData.put("tag", scenario.getTag());
        metaData.put("tagHash", scenario.getTagHash());

        String recordingName = scenario.getScenarioFilename();
        String errorPart = error ? "error_" : "";
        String fname = new String(recordingName + "_" + scenario.getExperimentId() + "_" + errorPart
                + String.format("%05d", position) + ".png");

        getGridFsTemplate().store(stream, fname, "image/png", metaData);
    }

    public InputStream getScreenshot(String recordingName, String experimentId, int step)
    {
        String fname = new String(recordingName + "_" + experimentId + "_" + String.format("%05d", step) + ".png");
        return getStreamByFilename(fname);
    }

    public InputStream getErrorScreenShot(String recordingName, String experimentId, int step)
    {
        String fname = new String(recordingName + "_" + experimentId + "_error_" + String.format("%05d", step) + ".png");
        return getStreamByFilename(fname);
    }

    @Nullable
    private InputStream getStreamByFilename(String fname)
    {
        GridFSDBFile file = getGridFsTemplate().findOne(new Query().addCriteria(Criteria.where("filename").is(fname)));
        if (file != null)
        {
            return file.getInputStream();
        }
        return null;
    }

    public void storeJMeterScenario(MongoDbScenario scenario, InputStream stream)
    {
        DBObject metaData = new BasicDBObject();
        metaData.put("recordingName", scenario.getRecordingName());
        metaData.put("recordingId", scenario.getRecordingId());
        metaData.put("experimentId", scenario.getExperimentId());
        metaData.put("tag", scenario.getTag());
        metaData.put("tagHash", scenario.getTagHash());

        String recordingName = scenario.getScenarioFilename();
        String fname = new String(recordingName + "_" + scenario.getExperimentId() + ".jmx");
        getGridFsTemplate().store(stream, fname, "text/xml", metaData);
    }

    public InputStream getJMeterScenario(String recordingName, String experimentId)
    {
        String fname = new String(recordingName + "_" + experimentId + ".jmx");
        return getStreamByFilename(fname);
    }
}
