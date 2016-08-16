package com.focusit.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by dkirpichenkov on 29.04.16.
 */
@Document
public class Recording
{
    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private String name;

    public String getId()
    {
        return id.toString();
    }

    public void setId(String id)
    {
        this.id = new ObjectId(id);
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
