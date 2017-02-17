package com.focusit.jsflight.server.service;

import com.focusit.jsflight.server.scenario.MongoDbScenario;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.InputStream;

@Service
public class MongoDbStorageService
{
    public static final String POSITION_FORMAT = "%06d";
    private static final Logger LOG = LoggerFactory.getLogger(MongoDbStorageService.class);
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
        LOG.info("Making screenshot");
        DBObject metaData = new BasicDBObject();
        metaData.put("recordingName", scenario.getRecordingName());
        metaData.put("recordingId", scenario.getRecordingId());
        metaData.put("experimentId", scenario.getExperimentId());
        String filename = error ? createErrorImageName(scenario.getRecordingName(), scenario.getExperimentId(),
                position) : createImageName(scenario.getRecordingName(), scenario.getExperimentId(), position);

        getGridFsTemplate().store(stream, filename, "image/png", metaData);
    }

    public InputStream getScreenshot(String recordingName, String experimentId, int step)
    {
        return getStreamByFilename(createImageName(recordingName, experimentId, step));
    }

    public InputStream getErrorScreenShot(String recordingName, String experimentId, int step)
    {
        return getStreamByFilename(createErrorImageName(recordingName, experimentId, step));
    }

    public void deleteScreenshots(String recordingName, String experimentId)
    {
        getGridFsTemplate().delete(
                new Query().addCriteria(Criteria.where("metadata.recordingName").is(recordingName)).addCriteria(
                        Criteria.where("metadata.experimentId").is(experimentId)));
    }

    @Nullable
    private InputStream getStreamByFilename(String fname)
    {
        GridFSDBFile file = getGridFsTemplate().findOne(new Query().addCriteria(Criteria.where("filename").is(fname)));

        return file != null ? file.getInputStream() : null;
    }

    public int getCountOfJMeterScenarios(String recordingName, String experimentId)
    {
        return getGridFsTemplate().find(
                new Query().addCriteria(Criteria.where("metadata.recordingName").is(recordingName)).addCriteria(
                        Criteria.where("metadata.experimentId").is(experimentId))).size();
    }

    public void storeJMeterScenario(MongoDbScenario scenario, InputStream stream, int index)
    {
        DBObject metaData = new BasicDBObject();
        metaData.put("recordingName", scenario.getRecordingName());
        metaData.put("recordingId", scenario.getRecordingId());
        metaData.put("experimentId", scenario.getExperimentId());
        metaData.put("index", index);

        String fname = createJmxName(scenario.getScenarioFilename(), scenario.getExperimentId(), index);
        getGridFsTemplate().store(stream, fname, "text/xml", metaData);
    }

    public InputStream getJMeterScenario(String recordingName, String experimentId, int index)
    {
        return getStreamByFilename(createJmxName(recordingName, experimentId, index));
    }

    private String createJmxName(String recordingName, String experimentId, int index)
    {
        return recordingName + '_' + experimentId + '_' + index + ".jmx";
    }

    private String createImageName(String recordingName, String experimentId, int position)
    {
        return String.format("%s_%s_%s.png", recordingName, experimentId, String.format(POSITION_FORMAT, position));
    }

    private String createErrorImageName(String recordingName, String experimentId, int position)
    {
        return String.format("%s_%s_error_%s.png", recordingName, experimentId,
                String.format(POSITION_FORMAT, position));
    }
}
