/*
 * Copyright 2013 Haulmont
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.haulmont.yarg.loaders.impl;

import com.haulmont.yarg.exception.DataLoadingException;
import com.haulmont.yarg.structure.BandData;
import com.haulmont.yarg.structure.ReportQuery;
import com.haulmont.yarg.util.db.QueryRunner;
import com.haulmont.yarg.util.db.ResultSetHandler;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads data from database using sql
 * You can use aliases for output values
 *
 * Example:
 * select login as "Login", password as "Password" from user where create_ts &gt; ${startDate}
 *
 * ${startDate} is alias of the input parameter, which will be passed to the query
 */
public class SqlDataLoader extends AbstractDbDataLoader {
    private DataSource dataSource;

    public SqlDataLoader(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Map<String, Object>> loadData(ReportQuery reportQuery, BandData parentBand, Map<String, Object> params) {
        List resList;
        final List<OutputValue> outputValues = new ArrayList<OutputValue>();

        String query = reportQuery.getScript();
        if (StringUtils.isBlank(query)) {
            return Collections.emptyList();
        }

        try {
            if (Boolean.TRUE.equals(reportQuery.getProcessTemplate())) {
                query = processQueryTemplate(query, parentBand, params);
            }
            final QueryPack pack = prepareQuery(query, parentBand, params);

            ArrayList<Object> resultingParams = new ArrayList<Object>();
            QueryParameter[] queryParameters = pack.getParams();
            for (QueryParameter queryParameter : queryParameters) {
                if (queryParameter.isSingleValue()) {
                    resultingParams.add(queryParameter.getValue());
                } else {
                    resultingParams.addAll(queryParameter.getMultipleValues());
                }
            }

            resList = runQuery(reportQuery, pack.getQuery(), resultingParams.toArray(), new ResultSetHandler<List>() {
                @Override
                public List handle(ResultSet rs) throws SQLException {
                    List<Object[]> resList = new ArrayList<Object[]>();

                    while (rs.next()) {
                        ResultSetMetaData metaData = rs.getMetaData();
                        if (outputValues.size() == 0) {
                            for (int columnIndex = 1; columnIndex <= metaData.getColumnCount(); columnIndex++) {
                                String columnName = metaData.getColumnLabel(columnIndex);
                                OutputValue outputValue = new OutputValue(columnName);
                                setCaseSensitiveSynonym(columnName, outputValue);
                                outputValues.add(outputValue);
                            }
                        }

                        Object[] values = new Object[metaData.getColumnCount()];
                        for (int columnIndex = 0; columnIndex < metaData.getColumnCount(); columnIndex++) {
                            values[columnIndex] = convertOutputValue(rs.getObject(columnIndex + 1));
                        }
                        resList.add(values);
                    }

                    return resList;
                }

                private void setCaseSensitiveSynonym(String columnName, OutputValue outputValue) {
                    Matcher matcher = Pattern.compile("(?i)as\\s*(" + columnName + ")").matcher(pack.getQuery());
                    if (matcher.find()) {
                        outputValue.setSynonym(matcher.group(1));
                    }
                }
            });
        } catch (DataLoadingException e) {
            throw e;
        } catch (Throwable e) {
            throw new DataLoadingException(String.format("An error occurred while loading data for data set [%s]", reportQuery.getName()), e);
        }

        return fillOutputData(resList, outputValues);
    }

    protected List runQuery(ReportQuery reportQuery, String queryString, Object[] params, ResultSetHandler<List> handler) throws SQLException {
        QueryRunner runner = new QueryRunner(getDataSource());
        return runner.query(queryString, params, handler);
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}