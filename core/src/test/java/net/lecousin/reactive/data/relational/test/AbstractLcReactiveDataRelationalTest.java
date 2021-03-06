package net.lecousin.reactive.data.relational.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import net.lecousin.reactive.data.relational.LcReactiveDataRelationalClient;
import net.lecousin.reactive.data.relational.model.LcEntityTypeInfo;
import net.lecousin.reactive.data.relational.model.LcEntityTypeInfo.JoinTableInfo;
import net.lecousin.reactive.data.relational.model.ModelUtils;
import net.lecousin.reactive.data.relational.query.SelectQuery;
import net.lecousin.reactive.data.relational.schema.RelationalDatabaseSchema;

@DataR2dbcTest
@EnableAutoConfiguration
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class AbstractLcReactiveDataRelationalTest {
	
	@Autowired
	protected LcReactiveDataRelationalClient lcClient;
	
	@Autowired
	protected DatabaseClient springClient;
	
	@BeforeEach
	public void initDatabase() {
		Collection<Class<?>> usedEntities = usedEntities();
		RelationalDatabaseSchema schema;
		if (usedEntities == null) {
			schema = lcClient.buildSchemaFromEntities();
		} else {
			List<Class<?>> entities = new LinkedList<>(usedEntities);
			for (Class<?> cl : usedEntities) {
				for (JoinTableInfo jt : LcEntityTypeInfo.get(cl).getJoinTables()) {
					Class<?> c = ModelUtils.getCollectionType(jt.getJoinForeignTable().getField());
					if (!entities.contains(c))
						entities.add(c);
				}
			}
			schema = lcClient.buildSchemaFromEntities(entities);
		}
		lcClient.dropCreateSchemaContent(schema).block();
	}
	
	protected Collection<Class<?>> usedEntities() {
		return null;
	}

	protected static class ExpectedEntity<T> {
		private ExpectedValue<T, ?>[] expectedValues;
		
		@SafeVarargs
		public ExpectedEntity(ExpectedValue<T, ?>... expectedValues) {
			this.expectedValues = expectedValues;
		}
	}
	
	protected static class ExpectedValue<S, T> {
		private Function<S, T> supplier;
		private T expected;
		
		public ExpectedValue(Function<S, T> supplier, T expected) {
			this.supplier = supplier;
			this.expected = expected;
		}
	}
	
	@SafeVarargs
	protected final <T> void expectEntities(Class<T> type, ExpectedEntity<T>... expected) {
		List<T> found =  SelectQuery.from(type, "entity").execute(lcClient).collectList().block();
		Assertions.assertEquals(expected.length, found.size());
		ArrayList<ExpectedEntity<T>> expectedEntities = new ArrayList<>(expected.length);
		Collections.addAll(expectedEntities, expected);
		StringBuilder error = new StringBuilder();
		for (T entity : found) {
			boolean matchFound = false;
			StringBuilder valuesFound = new StringBuilder();
			boolean first = true;
			for (Iterator<ExpectedEntity<T>> it = expectedEntities.iterator(); it.hasNext(); ) {
				ExpectedEntity<T> expectedEntity = it.next();
				boolean match = true;
				for (ExpectedValue<T, ?> expectedValue : expectedEntity.expectedValues) {
					Object value = expectedValue.supplier.apply(entity);
					if (first)
						valuesFound.append('<').append(value).append('>');
					if (!Objects.equals(value, expectedValue.expected))
						match = false;
				}
				first = false;
				if (match) {
					it.remove();
					matchFound = true;
					break;
				}
			}
			if (!matchFound) {
				error.append("Unexpected ").append(type.getSimpleName()).append(" with values ").append(valuesFound);
				for (ExpectedEntity<T> expectedEntity : expectedEntities) {
					error.append("\n - ");
					for (ExpectedValue<T, ?> expectedValue : expectedEntity.expectedValues)
						error.append('<').append(expectedValue.expected).append('>');
				}
				error.append('\n');
			}
		}
		if (!expectedEntities.isEmpty()) {
			error.append("Remaining expected entities: ");
			for (ExpectedEntity<T> expectedEntity : expectedEntities) {
				error.append("\n - ");
				for (ExpectedValue<T, ?> expectedValue : expectedEntity.expectedValues)
					error.append('<').append(expectedValue.expected).append('>');
			}
			error.append('\n');
		}
		if (error.length() > 0)
			throw new AssertionError(error.toString());
	}
	
}
