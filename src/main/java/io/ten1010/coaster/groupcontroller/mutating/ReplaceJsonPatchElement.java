package io.ten1010.coaster.groupcontroller.mutating;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

@Getter
@EqualsAndHashCode
@ToString
public class ReplaceJsonPatchElement {

    private String op;
    private String path;
    private JsonNode value;

    public ReplaceJsonPatchElement(String path, JsonNode value) {
        this.op = "replace";
        this.path = path;
        this.value = value;
    }

}
