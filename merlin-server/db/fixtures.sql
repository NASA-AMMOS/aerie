-- for mission_model.jar_path
set bytea_output = escape;

begin;
  insert into uploaded_file (path, name)
  values
    ('/models/foo.jar', 'Foo Adaptation'),
    ('/models/bar.jar', 'Bar Adaptation')
  returning id;

  insert into mission_model (mission, name, version, jar_id)
  values
    ('aerie', 'foo', '0.0.1', 1),
    ('aerie', 'bar', '0.0.1', 2)
  returning id;

  insert into activity_type (model_id, name, parameters)
  values
    (1, 'foo', '{"x": {"type": "int"}, "y": {"type": "string"}}');

  insert into plan (name, duration, start_time, model_id)
  values
    ('foo-test', '2 days', '2020-08-11T00:11:22', 1),
    ('foo-test-2', '1 day', '2021-08-11T00:11:22', 1)
  returning id;

  insert into activity (plan_id, start_offset, type, arguments)
  values
    (1, '0 seconds', 'foo', '{"x": 1, "y": "test_1"}'),
    (1, '50000 microseconds', 'foo', '{"x": 2, "y": "spawn"}'),
    (1, '50000 microseconds', 'foo', '{"x": 2}'),
    (1, '100000 microseconds', 'foo', '{"y": "test_2"}'),
    (1, '100000 microseconds', 'foo', '{"x": 2, "y": "test_2"}'),
    (1, '150000 microseconds', 'foo', '{"x": 3, "y": "test_3"}'),
    (2, '0 seconds', 'foo', '{"x": 1, "y": "test_1"}'),
    (2, '50000 microseconds', 'foo', '{"x": 2, "y": "spawn"}')
  returning id;

  insert into condition (name, summary, description, definition, plan_id, model_id)
  values
    ('constraint-a', 'a', 'aaa', '{}', 1, null),
    ('constraint-b', 'b', 'bbb', '{}', 1, null),
    ('constraint-c', 'c', 'ccc', '{}', 1, null)
  returning id;
end;
