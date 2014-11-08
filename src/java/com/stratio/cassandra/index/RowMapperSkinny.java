/*
 * Copyright 2014, Stratio.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stratio.cassandra.index;

import com.stratio.cassandra.index.schema.Columns;
import com.stratio.cassandra.index.schema.Schema;
import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.ColumnDefinition;
import org.apache.cassandra.db.ColumnFamily;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.db.Row;
import org.apache.cassandra.db.composites.CellName;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Sort;

import java.util.Comparator;

/**
 * {@link RowMapper} for skinny rows.
 *
 * @author Andres de la Pena <adelapena@stratio.com>
 */
public class RowMapperSkinny extends RowMapper
{
    private final Comparator<SearchResult> scoredDocumentComparator;
    private final SearchResultBuilder searchResultBuilder;

    /**
     * Builds a new {@link RowMapperSkinny} for the specified column family metadata, indexed column definition and {@link Schema}.
     *
     * @param metadata         The indexed column family metadata.
     * @param columnDefinition The indexed column definition.
     * @param schema           The mapping {@link Schema}.
     */
    RowMapperSkinny(CFMetaData metadata, ColumnDefinition columnDefinition, Schema schema)
    {
        super(metadata, columnDefinition, schema);

        final Comparator<DecoratedKey> partitionKeyComparator = partitionKeyMapper.comparator();
        this.scoredDocumentComparator = new Comparator<SearchResult>()
        {
            @Override
            public int compare(SearchResult o1, SearchResult o2)
            {
                DecoratedKey pk1 = o1.getPartitionKey();
                DecoratedKey pk2 = o2.getPartitionKey();
                return partitionKeyComparator.compare(pk1, pk2);
            }
        };

        this.searchResultBuilder = new SearchResultBuilderSkinny(partitionKeyMapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Columns columns(Row row)
    {
        Columns columns = new Columns();
        columns.addAll(partitionKeyMapper.columns(row));
        columns.addAll(regularCellsMapper.columns(row));
        return columns;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Document document(Row row)
    {
        DecoratedKey partitionKey = row.key;
        Document document = new Document();
        tokenMapper.addFields(document, partitionKey);
        partitionKeyMapper.addFields(document, partitionKey);
        schema.addFields(document, columns(row));
        return document;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Sort sort()
    {
        return new Sort(tokenMapper.sortFields());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CellName makeCellName(ColumnFamily columnFamily)
    {
        return metadata.comparator.makeCellName(columnDefinition.name.bytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RowComparator naturalComparator()
    {
        return new RowComparatorNatural();
    }

    @Override
    public Comparator<SearchResult> scoredDocumentsComparator() {
        return scoredDocumentComparator;
    }

    @Override
    public String toString(SearchResult searchResult) {
        DecoratedKey partitionKey = searchResult.getPartitionKey();
        return partitionKeyMapper.toString(partitionKey);
    }

    @Override
    public SearchResultBuilder searchResultBuilder() {
        return searchResultBuilder;
    }
}