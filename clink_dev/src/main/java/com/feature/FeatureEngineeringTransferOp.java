package com.feature;

import org.apache.flink.api.common.functions.RichGroupReduceFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.ml.api.misc.param.Params;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;

import com.alibaba.alink.operator.batch.BatchOperator;
import com.alibaba.alink.pipeline.PipelineStageBase;

import java.util.ArrayList;
import java.util.List;

import static com.alibaba.alink.pipeline.ModelExporterUtils.loadStagesFromPipelineModel;
import static com.feature.stageparser.BaseStageParser.STAGE_PARSERS;


public class FeatureEngineeringTransferOp extends BatchOperator<FeatureEngineeringTransferOp>
        implements FeatureEngineeringTransferParams<FeatureEngineeringTransferOp> {
    public FeatureEngineeringTransferOp() {
        this(new Params());
    }

    public FeatureEngineeringTransferOp(Params params) {
        super(params);
    }

    @Override
    public FeatureEngineeringTransferOp linkFrom(BatchOperator<?>... inputs) {
        final String[] names = inputs[0].getColNames();
        final TypeInformation[] types = inputs[0].getColTypes();


        DataSet<Row> ret = inputs[0].getDataSet().reduceGroup(new RichGroupReduceFunction<Row, Row>() {
            private List<Row> modelRows = new ArrayList<>();

            @Override
            public void reduce(Iterable<Row> iterable, Collector<Row> collector) throws Exception {
                for (Row row : iterable) {
                    modelRows.add(row);
                }
                List<Tuple3<PipelineStageBase<?>, TableSchema, List<Row>>>
                        stages = loadStagesFromPipelineModel(modelRows, new TableSchema(names, types));

                for (int i = 0; i < stages.size(); ++i) {
                    Tuple3<PipelineStageBase<?>, TableSchema, List<Row>> t3 = stages.get(i);
//                    collector.collect(Row.of(STAGE_PARSERS.get(t3.f0.getClass().toString()).serializeModelToJson(t3)
//                            + (i == stages.size() - 1 ? "" : ",")));
                    collector.collect(Row.of(STAGE_PARSERS.get(t3.f0.getClass().toString()).serializeModelToJson(t3)));
                }
            }
        });
        setOutput(ret, new TableSchema(new String[]{"out"}, new TypeInformation[]{Types.STRING}));
        return this;
    }
}
