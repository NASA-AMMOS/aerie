package gov.nasa.jpl.aerie.e2e.types;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public sealed interface ValueSchema {
  // Constants for the Value Schemas that take no parameters
  ValueSchemaBoolean VALUE_SCHEMA_BOOLEAN = new ValueSchemaBoolean();
  ValueSchemaDuration VALUE_SCHEMA_DURATION = new ValueSchemaDuration();
  ValueSchemaInt VALUE_SCHEMA_INT = new ValueSchemaInt();
  ValueSchemaPath VALUE_SCHEMA_PATH = new ValueSchemaPath();
  ValueSchemaReal VALUE_SCHEMA_REAL = new ValueSchemaReal();
  ValueSchemaString VALUE_SCHEMA_STRING = new ValueSchemaString();

  static ValueSchema fromJSON(JsonObject json) {
    switch (json.getString("type")) {
      case "boolean" -> {return VALUE_SCHEMA_BOOLEAN;}
      case "duration" -> {return VALUE_SCHEMA_DURATION;}
      case "int" -> {return VALUE_SCHEMA_INT;}
      case "path" -> {return VALUE_SCHEMA_PATH;}
      case "real" -> {return VALUE_SCHEMA_REAL;}
      case "series" -> {return new ValueSchemaSeries(ValueSchema.fromJSON(json.getJsonObject("items")));}
      case "string" -> {return VALUE_SCHEMA_STRING;}
      case "struct" -> {
        final var items = new HashMap<String, ValueSchema>();
        final var itemsJson = json.getJsonObject("items");
        for(final var item : itemsJson.keySet()){
          items.put(item, ValueSchema.fromJSON(itemsJson.getJsonObject(item)));
        }
        return new ValueSchemaStruct(items);
      }
      case "variant" -> {
        final var variants = json.getJsonArray("variants")
                                 .getValuesAs((JsonObject v) -> new Variant(v.getString("key"), v.getString("label")));
        return new ValueSchemaVariant(variants);
      }
      default -> throw new IllegalArgumentException("Cannot determine ValueSchema from JSON");
    }
  }

  JsonObject toJson();

  record ValueSchemaBoolean() implements ValueSchema {
    @Override
    public JsonObject toJson() {
      return Json.createObjectBuilder().add("type", "boolean").build();
    }
  }

  record ValueSchemaDuration() implements ValueSchema {
    @Override
    public JsonObject toJson() {
      return Json.createObjectBuilder().add("type", "duration").build();
    }
  }

  record ValueSchemaInt() implements ValueSchema {
    @Override
    public JsonObject toJson() {
      return Json.createObjectBuilder().add("type", "int").build();
    }
  }

  record ValueSchemaPath() implements ValueSchema {
    @Override
    public JsonObject toJson() {
      return Json.createObjectBuilder().add("type", "path").build();
    }
  }

  record ValueSchemaReal() implements ValueSchema {
    @Override
    public JsonObject toJson() {
      return Json.createObjectBuilder().add("type", "real").build();
    }
  }

  record ValueSchemaSeries(ValueSchema items) implements ValueSchema {
    @Override
    public JsonObject toJson() {
      return Json.createObjectBuilder()
                 .add("type", "series")
                 .add("items", items.toJson())
                 .build();
    }
  }

  record ValueSchemaString() implements ValueSchema {
    @Override
    public JsonObject toJson() {
      return Json.createObjectBuilder().add("type", "string").build();
    }
  }

  record ValueSchemaStruct(Map<String, ValueSchema> items) implements ValueSchema {
    @Override
    public boolean equals(Object o){
      if (!(o instanceof final ValueSchemaStruct other)) return false;

      if(this.items.size() != other.items.size()) return false;
      for(final var itemKey : this.items().keySet()){
        if(!other.items.containsKey(itemKey)) return false;
        if(!this.items.get(itemKey).equals(other.items.get(itemKey))) return false;
      }
      return true;
    }

    @Override
    public JsonObject toJson() {
      final var itemsBuilder = Json.createObjectBuilder();
      items.forEach((k, v) -> itemsBuilder.add(k, v.toJson()));

      return Json.createObjectBuilder()
                 .add("type", "struct")
                 .add("items", itemsBuilder)
                 .build();
    }
  }

  record ValueSchemaVariant(List<Variant> variants) implements ValueSchema {
    @Override
    public JsonObject toJson() {
      final var variantsBuilder = Json.createArrayBuilder();
      variants.forEach(v -> variantsBuilder.add(v.toJson()));

      return Json.createObjectBuilder()
                 .add("type", "variant")
                 .add("variants", variantsBuilder)
                 .build();
    }
  }
  record Variant(String key, String label) {
    public JsonObject toJson() {
      return Json.createObjectBuilder().add("key", key).add("label", label).build();
    }
  }
}