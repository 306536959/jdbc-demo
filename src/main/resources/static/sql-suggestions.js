window.addEventListener('DOMContentLoaded', function() {
    const sqlInput = document.getElementById('sqlQuery');
    const suggestionsContainer = document.getElementById('sqlSuggestions');
    let activeIndex = -1;
    let databaseMetadata = null;
    const strategyIdInput = document.querySelector('input[name="strategyId"]');

    // SQL关键字（固定不变）
    // SQL关键字（固定不变）- 已移除与 sqlFunctions 冲突的项
    const sqlKeywords = [
        'SELECT', 'FROM', 'WHERE', 'INSERT', 'INTO', 'VALUES',
        'UPDATE', 'SET', 'DELETE', 'JOIN', 'ON', 'LEFT JOIN',
        'RIGHT JOIN', 'FULL JOIN', 'GROUP BY', 'HAVING', 'ORDER BY',
        'LIMIT', 'OFFSET', 'DISTINCT', 'AS', 'AND', 'OR', 'NOT',
        'BETWEEN', 'LIKE', 'IN', 'IS NULL', 'IS NOT NULL', 'ASC', 'DESC'
    ];
// SQL 函数建议列表
    const sqlFunctions = [
        // 时间函数
        'NOW()', 'CURDATE()', 'CURRENT_DATE()', 'CURRENT_TIME()', 'CURRENT_TIMESTAMP()',
        'DATE()', 'TIME()', 'YEAR()', 'MONTH()', 'DAY()', 'HOUR()', 'MINUTE()', 'SECOND()',

        // 聚合函数
        'COUNT(*)', 'COUNT(?)', 'SUM(?)', 'AVG(?)', 'MAX(?)', 'MIN(?)',

        // 字符串函数
        'CONCAT(?, ?)', 'UPPER(?)', 'LOWER(?)', 'TRIM(?)', 'SUBSTRING(?, ?, ?)', 'LENGTH(?)',

        // 数学函数
        'ABS(?)', 'ROUND(?, ?)', 'CEIL(?)', 'FLOOR(?)', 'RAND()', 'POWER(?, ?)', 'SQRT(?)',

        // 条件函数
        'IFNULL(?, ?)', 'COALESCE(?, ?)', 'CASE WHEN THEN END'
    ];
    // 连接成功后获取数据库元数据
    if (strategyIdInput && strategyIdInput.value) {
        fetch(`/demo/api/database/metadata?strategyId=${strategyIdInput.value}`)
            .then(response => {
                if (!response.ok) throw new Error('获取数据库元数据失败');
                return response.json();
            })
            .then(metadata => {
                databaseMetadata = metadata;
                try {
                    displayDbInfo(metadata);
                    displayTables(metadata);
                } catch (e) {
                    console.error('渲染失败:', e);
                }

                console.log('数据库元数据加载成功', metadata);
            })
            .catch(error => {
                console.error('获取数据库元数据失败:', error);
                alert('无法获取数据库结构信息，SQL自动补全将使用默认数据');
            });
    }


    // 获取当前光标位置的行和列
    function getCursorLineAndColumn(textarea) {
        const text = textarea.value;
        const cursorPos = textarea.selectionStart;
        const lineStart = text.lastIndexOf('\n', cursorPos - 1) + 1;
        const lineEnd = text.indexOf('\n', cursorPos);
        const line = text.substring(lineStart, lineEnd === -1 ? text.length : lineEnd);
        const column = cursorPos - lineStart;

        return {
            line: line,
            column: column,
            lineStart: lineStart,
            lineEnd: lineEnd === -1 ? text.length : lineEnd
        };
    }

    // 获取当前单词
    function getCurrentWord(line, column) {
        // 向前查找单词开始
        let start = column;
        while (start > 0 && !/\s/.test(line[start - 1])) {
            start--;
        }

        // 向后查找单词结束
        let end = column;
        while (end < line.length && !/\s/.test(line[end])) {
            end++;
        }

        return {
            word: line.substring(start, end),
            start: start,
            end: end
        };
    }

    // 获取SQL上下文（简化版）
    function getSQLContext(text, cursorPos) {
        const beforeCursor = text.substring(0, cursorPos);

        // 简单识别FROM子句后的表名
        const fromRegex = /FROM\s+([\w,\s]+)/gi;
        let fromMatch;
        let tables = [];

        while ((fromMatch = fromRegex.exec(beforeCursor)) !== null) {
            const tableList = fromMatch[1].split(',');
            tables = tables.concat(tableList.map(t => t.trim()));
        }

        // 简单识别WHERE子句后的条件
        const whereRegex = /WHERE\s+([\w\s.]+)$/gi;
        const whereMatch = whereRegex.exec(beforeCursor);
        const inWhereClause = whereMatch !== null;

        // 新增：尝试识别"表名."后紧接光标的情况（即 . 后面是光标）
        const dotRegex = /(\w+)\.\s*$/g;  // 匹配类似 "table."
        const dotMatch = dotRegex.exec(beforeCursor);
        const tableAfterDot = dotMatch ? dotMatch[1] : null;

        return {
            tables: [...new Set(tables)],  // 去重
            inWhereClause: inWhereClause,
            currentTable: dotMatch ? dotMatch[1] : null,  // 只有在 "." 后才返回当前表名
            tableAfterDot: tableAfterDot
        };
    }


    // 显示提示
    function showSuggestions() {
        const cursorPos = sqlInput.selectionStart;
        const { line, column } = getCursorLineAndColumn(sqlInput);
        const { word } = getCurrentWord(line, column);
        const context = getSQLContext(sqlInput.value, cursorPos);

        if (word.length < 1) {
            suggestionsContainer.style.display = 'none';
            return;
        }

        const suggestions = [];

        // 仅在 "xxx." 后显示列名建议
        if (context.tableAfterDot) {
            const columns = getColumnsForTable(context.tableAfterDot);
            if (columns) {
                columns.forEach(column => {
                    suggestions.push({
                        type: 'column',
                        value: `${context.tableAfterDot}.${column}`,
                        display: column
                    });
                });
            }
        } else {
            // 正常提示关键字、表名、函数
            sqlKeywords.forEach(keyword => {
                if (keyword.toLowerCase().startsWith(word.toLowerCase())) {
                    suggestions.push({ type: 'keyword', value: keyword });
                }
            });

            const tables = getTables();
            tables.forEach(table => {
                if (table.toLowerCase().startsWith(word.toLowerCase())) {
                    suggestions.push({ type: 'table', value: table });
                }
            });

            // 添加函数提示
            sqlFunctions.forEach(func => {
                if (func.toLowerCase().startsWith(word.toLowerCase())) {
                    console.log('匹配到函数:', func); // 调试用
                    suggestions.push({ type: 'function', value: func });
                }
            });
        }

        renderSuggestions(suggestions, cursorPos);
    }




    // 从元数据获取表列表
    function getTables() {
        if (databaseMetadata && databaseMetadata.tables) {
            return databaseMetadata.tables.map(t => t.name);
        }
        // 默认返回模拟数据
        return ['users', 'orders', 'products', 'categories', 'suppliers'];
    }

    // 从元数据获取表的列
    function getColumnsForTable(tableName) {
        if (databaseMetadata && databaseMetadata.tables) {
            const table = databaseMetadata.tables.find(t => t.name.toLowerCase() === tableName.toLowerCase());
            if (table) {
                return table.columns.map(c => c.name);
            }
        }
        // 默认返回模拟数据
        const mockColumns = {
            users: ['id', 'name', 'email', 'password', 'created_at'],
            orders: ['order_id', 'user_id', 'product_id', 'quantity', 'total_amount'],
            products: ['product_id', 'name', 'description', 'price', 'stock'],
            categories: ['category_id', 'name', 'description'],
            suppliers: ['supplier_id', 'name', 'contact_person', 'email']
        };
        return mockColumns[tableName] || [];
    }

    // 获取所有列
    function getAllColumns() {
        if (databaseMetadata && databaseMetadata.tables) {
            const columns = [];
            databaseMetadata.tables.forEach(table => {
                table.columns.forEach(column => {
                    columns.push({
                        table: table.name,
                        name: column.name
                    });
                });
            });
            return columns;
        }
        // 默认返回模拟数据
        return [
            { table: 'users', name: 'id' },
            { table: 'users', name: 'name' },
            { table: 'users', name: 'email' },
            { table: 'orders', name: 'order_id' },
            { table: 'orders', name: 'user_id' },
            { table: 'products', name: 'product_id' },
            { table: 'products', name: 'name' },
            { table: 'products', name: 'price' }
        ];
    }

    // 渲染建议
    function renderSuggestions(suggestions, cursorPos) {
        suggestionsContainer.innerHTML = '';

        if (suggestions.length === 0) {
            suggestionsContainer.innerHTML = '<div class="sql-suggestion text-muted">没有匹配的建议</div>';
            suggestionsContainer.style.display = 'block';
            return;
        }

        suggestions.forEach((suggestion, index) => {
            const div = document.createElement('div');
            div.className = 'sql-suggestion';
            div.dataset.value = suggestion.value;
            div.dataset.type = suggestion.type;

            const displayValue = suggestion.display || suggestion.value;

            let htmlContent = `<span class="sql-${suggestion.type}">${displayValue}</span>`;
            if (suggestion.type === 'column') {
                htmlContent += ` <span class="text-muted">(${suggestion.value.split('.')[0]})</span>`;
            }

            div.innerHTML = htmlContent;

            div.addEventListener('click', () => {
                insertSuggestion(suggestion.value);
            });

            suggestionsContainer.appendChild(div);
        });

        const rect = sqlInput.getBoundingClientRect();
        const cursorRect = getCursorPosition(sqlInput, cursorPos);
        suggestionsContainer.style.left = `${rect.left + cursorRect.left}px`;
        suggestionsContainer.style.top = `${rect.top + cursorRect.top + cursorRect.height}px`;
        suggestionsContainer.style.display = 'block';

        activeIndex = -1;
    }

    // 获取光标位置
    function getCursorPosition(textarea, pos) {
        // 创建一个临时元素来计算位置
        const div = document.createElement('div');
        const style = div.style;
        const computed = window.getComputedStyle(textarea);

        // 复制 textarea 的样式
        style.fontFamily = computed.fontFamily;
        style.fontSize = computed.fontSize;
        style.fontWeight = computed.fontWeight;
        style.lineHeight = computed.lineHeight;
        style.whiteSpace = 'pre-wrap';
        style.wordWrap = 'break-word';

        // 设置宽度与 textarea 相同
        style.width = `${textarea.offsetWidth - 2}px`;

        // 将临时元素添加到页面但不可见
        style.position = 'absolute';
        style.top = '-9999px';
        document.body.appendChild(div);

        // 获取 textarea 的内容和光标位置
        const text = textarea.value.substring(0, pos);

        // 处理换行符
        div.textContent = text;

        // 确保总是创建span元素
        div.innerHTML = div.innerHTML.replace(/\n/g, '<br>');

        const span = document.createElement('span');
        span.textContent = ' ';
        div.appendChild(span);

        // 获取光标位置
        const rect = span.getBoundingClientRect();
        const result = {
            left: rect.left - document.body.getBoundingClientRect().left,
            top: rect.top - document.body.getBoundingClientRect().top,
            height: parseInt(computed.lineHeight)
        };

        // 移除临时元素
        document.body.removeChild(div);

        return result;
    }

    // 插入建议
    function insertSuggestion(value) {
        const cursorPos = sqlInput.selectionStart;
        const { line, column, lineStart, lineEnd } = getCursorLineAndColumn(sqlInput);
        const { start, end } = getCurrentWord(line, column);

        const before = sqlInput.value.substring(0, lineStart + start);
        const after = sqlInput.value.substring(lineStart + end);

        sqlInput.value = before + value + after;
        sqlInput.focus();

        // 设置新的光标位置
        const newCursorPos = before.length + value.length;
        sqlInput.selectionStart = newCursorPos;
        sqlInput.selectionEnd = newCursorPos;

        suggestionsContainer.style.display = 'none';
    }

    // 高亮选项
    function highlightOption(index) {
        const options = suggestionsContainer.querySelectorAll('.sql-suggestion');

        options.forEach((option, i) => {
            option.classList.toggle('active', i === index);
        });

        // 新增：将当前高亮项滚动到可视区域
        if (options[index]) {
            const containerRect = suggestionsContainer.getBoundingClientRect();
            const itemRect = options[index].getBoundingClientRect();

            // 如果当前项在容器上方不可见
            if (itemRect.top < containerRect.top) {
                options[index].scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }
            // 如果当前项在容器下方不可见
            else if (itemRect.bottom > containerRect.bottom) {
                options[index].scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }
        }
    }

    // 事件监听

    sqlInput.addEventListener('input', showSuggestions);
    sqlInput.addEventListener('click', showSuggestions);
    sqlInput.addEventListener('focus', showSuggestions);
    sqlInput.addEventListener('blur', () => {
        setTimeout(() => {
            suggestionsContainer.style.display = 'none';
        }, 200);
    });

    // 键盘导航
    document.addEventListener('keydown', (e) => {
        if (!suggestionsContainer || suggestionsContainer.style.display === 'none') {
            return;
        }

        const options = suggestionsContainer.querySelectorAll('.sql-suggestion');
        const count = options.length;

        if (count === 0) return;

        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault();
                activeIndex = (activeIndex + 1) % count;
                highlightOption(activeIndex);
                break;
            case 'ArrowUp':
                e.preventDefault();
                activeIndex = (activeIndex - 1 + count) % count;
                highlightOption(activeIndex);
                break;
            case 'Tab':
            case 'Enter':
                e.preventDefault();
                if (activeIndex >= 0) {
                    const suggestion = options[activeIndex];
                    insertSuggestion(suggestion.dataset.value);
                }
                break;
            case 'Escape':
                e.preventDefault();
                suggestionsContainer.style.display = 'none';
                break;
        }
    });
    // 渲染数据库信息
    function displayDbInfo(metadata) {
        console.log('开始渲染数据库信息', metadata);
        const container = document.getElementById('dbInfo');
        const urlMatch = metadata.url.match(/\/\/([^:\/]+)/);
        const ipAddress = urlMatch ? urlMatch[1] : '未知';

        container.innerHTML = `
        <div class="db-info">
            <h3>数据库信息</h3>
            <div class="db-info-row">
                <span><strong>IP地址:</strong> ${ipAddress}</span>
                <span><strong>数据库类型:</strong> ${metadata.databaseProductName || 'MySQL'}</span>
                <span><strong>数据库名称:</strong> ${metadata.databaseName || '未知'}</span>
                <span><strong>用户名:</strong> ${metadata.userName || 'root'}</span>
                <span><strong>表数量:</strong> ${metadata.tables ? metadata.tables.length : 0}</span>
                <span><strong>版本:</strong> ${metadata.databaseProductVersion || '未知'}</span>
            </div>
        </div>
    `;
    }


    // 渲染表名
    function displayTables(metadata) {
        console.log('开始渲染表名列表', metadata);
        const tableListContainer = document.getElementById('tableList');
        const sqlQueryInput = document.getElementById('sqlQuery');
        tableListContainer.innerHTML = '';

        if (!metadata.tables || metadata.tables.length === 0) {
            tableListContainer.innerHTML = '<li class="text-muted">没有可用的表</li>';
            return;
        }

        metadata.tables.forEach(table => {
            const li = document.createElement('li');
            li.className = 'table-item';
            li.innerHTML = `
                <span class="sql-table">${table.name}</span> 
                <span class="text-muted text-sm">(${table.columns ? table.columns.length : 0}列)</span>
            `;
            li.addEventListener('click', () => {
                sqlQueryInput.value = `SELECT * FROM ${table.name};`;
            });
            tableListContainer.appendChild(li);
        });
    }
});