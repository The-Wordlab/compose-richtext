package com.halilibo.richtext.markdown

import androidx.compose.runtime.Composable
import com.halilibo.richtext.markdown.node.AstNode
import com.halilibo.richtext.markdown.node.AstTableBody
import com.halilibo.richtext.markdown.node.AstTableCell
import com.halilibo.richtext.markdown.node.AstTableHeader
import com.halilibo.richtext.markdown.node.AstTableRow
import com.halilibo.richtext.ui.RichTextScope
import com.halilibo.richtext.ui.Table

@Composable
internal fun RichTextScope.RenderTable(node: AstNode) {
  val headerCells = node.filterChildrenType<AstTableHeader>()
    .firstOrNull()
    ?.filterChildrenType<AstTableRow>()
    ?.firstOrNull()
    ?.filterChildrenType<AstTableCell>()
    ?.toList()
    .orEmpty()
  val bodyRows = node.filterChildrenType<AstTableBody>()
    .firstOrNull()
    ?.filterChildrenType<AstTableRow>()
    ?.toList()
    .orEmpty()
  val cells = headerCells + bodyRows.flatMap { it.filterChildrenType<AstTableCell>().toList() }
  val offsets = tailOffsetsOrNull(cells)

  Table(
    headerRow = {
      headerCells.forEach { tableCell ->
        cell {
          ProvideTailOffset(offsets.tailOffsetOf(cells, tableCell)) {
            MarkdownRichText(tableCell)
          }
        }
      }
    }
  ) {
    bodyRows.forEach { tableRow ->
      row {
        tableRow.filterChildrenType<AstTableCell>()
          .forEach { tableCell ->
            cell {
              ProvideTailOffset(offsets.tailOffsetOf(cells, tableCell)) {
                MarkdownRichText(tableCell)
              }
            }
          }
      }
    }
  }
}
