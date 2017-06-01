/*
 Copyright 2013 Red Hat, Inc. and/or its affiliates.
 
 This file is part of lightblue.
 
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package savemgo.nomad.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NamedParameterStatement {

	public static final char EMPTY = ' ';

	private final PreparedStatement statement;
	private Map<String, int[]> variablesMap;

	public NamedParameterStatement(Connection connection, String query) throws SQLException {
		String parsedQuery = prepare(query);
		// LOGGER.debug("NamedParameterStatement constructor-> query:{}
		// parsedQuery:{}", query, parsedQuery);
		statement = connection.prepareStatement(parsedQuery);
	}

	private int findColon(int start, StringBuffer query) {
		char quotationStart = EMPTY;
		boolean inSingleQuotes = false;
		boolean inDoubleQuotes = false;
		for (int i = start; i < query.length(); i++) {
			char c = query.charAt(i);

			// for escaped (checking if it necessary)
			char before = EMPTY;
			if (i != 0) {
				before = query.charAt(i - 1);
			}
			// quote or double quote should be ignored
			if ((c == '"' || c == '\'') && before != '\\') {
				if (c == '\'') {
					if (inSingleQuotes) {
						inSingleQuotes = false;
						if (quotationStart == '\'') {
							quotationStart = EMPTY;
							inDoubleQuotes = false;//
						}
					} else {
						inSingleQuotes = true;
						if (quotationStart == EMPTY) {
							quotationStart = c;
						}
					}

				} else if (c == '"') {
					if (inDoubleQuotes) {
						inDoubleQuotes = false;
						if (quotationStart == '"') {
							quotationStart = EMPTY;
							inSingleQuotes = false;
						}
					} else {
						inDoubleQuotes = true;
						if (quotationStart == EMPTY) {
							quotationStart = c;
						}
					}
				}

			}
			if ((quotationStart == '\'' && inSingleQuotes) || (quotationStart == '"' && inDoubleQuotes)) {
				continue;
			}

			// get parameter
			if (c == ':' && query.length() > i + 1 && (query.charAt(i + 1) != '=') && (query.charAt(i + 1) != ' ')) {
				return i;
			}
		}
		return -1;
	}

	private static int findEndofVariable(StringBuffer sql, int idx) {
		int i = idx + 1;
		while (i < sql.length() && Character.isJavaIdentifierPart(sql.charAt(i))) {
			i++;
		}
		return i;
	}

	protected String prepare(String query) {
		StringBuffer sql = new StringBuffer(query);
		HashMap<String, List<Integer>> variables = new HashMap<>();
		int idx = findColon(0, sql);
		int variableNumber = 1;

		while (idx != -1) {
			int end = findEndofVariable(sql, idx);
			String name = sql.substring(idx + 1, end);
			sql = sql.replace(idx, end, "?");

			List<Integer> variableList = variables.get(name);
			if (variableList == null) {
				variableList = new ArrayList<>();
				variables.put(name, variableList);
			}
			variableList.add(variableNumber);
			variableNumber++;

			idx = findColon(idx + 1, sql);
		}

		variablesMap = new HashMap<>(variables.size());
		for (Map.Entry<String, List<Integer>> entry : variables.entrySet()) {
			List<Integer> list = entry.getValue();
			int[] indexes = new int[list.size()];
			int i = 0;
			for (Integer x : list) {
				indexes[i++] = x;
			}
			variablesMap.put(entry.getKey(), indexes);
		}

		return sql.toString();
	}

	private int[] getVariableIndexes(String name) {
		int[] indexes = variablesMap.get(name);
		if (indexes == null) {
			// LOGGER.debug("getVariableIndexes didn't find the variable:{} in
			// :{}", name, variablesMap.keySet());
			throw new IllegalStateException("Parameter not found: " + name);
		}
		return indexes;
	}

	public void setObject(String name, Object value) throws SQLException {
		int[] indexes = getVariableIndexes(name);
		for (int index : indexes) {
			statement.setObject(index, value);
		}
	}

	public void setString(String name, String value) throws SQLException {
		int[] indexes = getVariableIndexes(name);
		for (int index : indexes) {
			statement.setString(index, value);
		}
	}

	public void setInt(String name, int value) throws SQLException {
		int[] indexes = getVariableIndexes(name);
		for (int index : indexes) {
			statement.setInt(index, value);
		}
	}

	public void setBigDecimal(String name, BigDecimal value) throws SQLException {
		int[] indexes = getVariableIndexes(name);
		for (int index : indexes) {
			statement.setBigDecimal(index, value);
		}
	}

	public void setLong(String name, long value) throws SQLException {
		int[] indexes = getVariableIndexes(name);
		for (int index : indexes) {
			statement.setLong(index, value);
		}
	}

	public void setTimestamp(String name, Timestamp value) throws SQLException {
		int[] indexes = getVariableIndexes(name);
		for (int index : indexes) {
			statement.setTimestamp(index, value);
		}
	}

	public void setTime(String name, Time value) throws SQLException {
		int[] indexes = getVariableIndexes(name);
		for (int index : indexes) {
			statement.setTime(index, value);
		}
	}

	public void setDouble(String name, double value) throws SQLException {
		int[] indexes = getVariableIndexes(name);
		for (int index : indexes) {
			statement.setDouble(index, value);
		}
	}

	public void setBoolean(String name, boolean value) throws SQLException {
		int[] indexes = getVariableIndexes(name);
		for (int index : indexes) {
			statement.setBoolean(index, value);
		}
	}

	public void setBytes(String name, byte[] value) throws SQLException {
		int[] indexes = getVariableIndexes(name);
		for (int index : indexes) {
			statement.setBytes(index, value);
		}
	}

	public boolean execute() throws SQLException {
		return statement.execute();
	}

	public ResultSet executeQuery() throws SQLException {
		return statement.executeQuery();
	}

	public int executeUpdate() throws SQLException {
		return statement.executeUpdate();
	}

	public void close() throws SQLException {
		statement.close();
	}

	public PreparedStatement getPrepareStatement() {
		return statement;
	}

}