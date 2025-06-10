window.addEventListener('DOMContentLoaded', function () {
    const strategyInput = document.getElementById('strategyId');
    const datalistOptions = document.getElementById('datalistOptions');
    let allStrategies = []; // 存储策略对象 { strategyCode, strategyName }
    let currentActiveIndex = -1;

    // 加载策略列表
    fetch('/api/strategies')
        .then(response => {
            if (!response.ok) throw new Error('接口调用失败');
            return response.json();
        })
        .then(strategies => {
            if (!Array.isArray(strategies)) throw new Error('返回不是数组');

            allStrategies = strategies;

            // 恢复已选择的值（如果有）
            const selectedValue = strategyInput.getAttribute('th:value');
            if (selectedValue) {
                strategyInput.value = selectedValue;
                const selectedStrategy = strategies.find(s => s.strategyCode === selectedValue);
                if (selectedStrategy) {
                    strategyInput.setAttribute('title', `${selectedStrategy.strategyName} (${selectedStrategy.strategyCode})`);
                }
            }
        })
        .catch(error => {
            console.error('加载策略失败:', error);
            alert('无法加载策略列表，请检查后端服务是否启动');
        });

    // 输入事件处理
    strategyInput.addEventListener('input', function () {
        const query = this.value.toLowerCase();
        showOptions(query);
    });

    // 焦点事件处理
    strategyInput.addEventListener('focus', function () {
        if (this.value.trim()) {
            showOptions(this.value.toLowerCase());
        } else {
            showOptions(''); // 显示所有选项
        }
    });

    // 失去焦点事件处理
    strategyInput.addEventListener('blur', function () {
        // 延迟隐藏选项，以便能够点击选项
        setTimeout(() => {
            datalistOptions.style.display = 'none';
        }, 200);
    });

    // 键盘导航
    strategyInput.addEventListener('keydown', function (e) {
        const options = datalistOptions.querySelectorAll('.datalist-option');
        const optionsCount = options.length;

        if (optionsCount === 0) return;

        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault();
                currentActiveIndex = (currentActiveIndex + 1) % optionsCount;
                highlightOption(currentActiveIndex);
                break;
            case 'ArrowUp':
                e.preventDefault();
                currentActiveIndex = (currentActiveIndex - 1 + optionsCount) % optionsCount;
                highlightOption(currentActiveIndex);
                break;
            case 'Enter':
                e.preventDefault();
                if (currentActiveIndex >= 0) {
                    selectOption(options[currentActiveIndex]);
                }
                break;
            case 'Escape':
                datalistOptions.style.display = 'none';
                break;
        }
    });

    // 显示选项
    function showOptions(query) {
        datalistOptions.innerHTML = '';

        const filteredStrategies = query
            ? allStrategies.filter(s =>
                s.strategyName.toLowerCase().includes(query) ||
                s.strategyCode.toLowerCase().includes(query)
            )
            : allStrategies;

        if (filteredStrategies.length === 0) {
            datalistOptions.innerHTML = '<div class="datalist-option text-gray-500">没有找到匹配项</div>';
            datalistOptions.style.display = 'block';
            return;
        }

        filteredStrategies.forEach(strategy => {
            const option = document.createElement('div');
            option.className = 'datalist-option';
            option.dataset.value = strategy.strategyCode; // 实际值
            option.textContent = `${strategy.strategyName} (${strategy.strategyCode})`;

            // 高亮匹配的文本
            if (query) {
                const regex = new RegExp(`(${query})`, 'gi');
                const highlightedName = strategy.strategyName.replace(regex, '<span style="font-weight:bold;background-color:#ffffcc">$1</span>');
                const highlightedCode = strategy.strategyCode.replace(regex, '<span style="font-weight:bold;background-color:#ffffcc">$1</span>');
                option.innerHTML = `${highlightedName} (<span class="sql-column">${highlightedCode}</span>)`;
            }

            option.addEventListener('click', function () {
                selectOption(this);
            });

            datalistOptions.appendChild(option);
        });

        datalistOptions.style.display = 'block';
        currentActiveIndex = -1;
    }

    // 高亮选项
    function highlightOption(index) {
        const options = datalistOptions.querySelectorAll('.datalist-option');

        options.forEach((option, i) => {
            if (i === index) {
                option.classList.add('active');
                option.scrollIntoView({ block: 'nearest' });
            } else {
                option.classList.remove('active');
            }
        });

        strategyInput.value = options[index].dataset.value;
    }

    // 选择选项
    function selectOption(option) {
        strategyInput.value = option.dataset.value;
        datalistOptions.style.display = 'none';
        strategyInput.focus(); // 保持焦点

        const selectedStrategyId = option.dataset.value;

        // 请求数据库元数据
        fetch(`/api/database/metadata?strategyId=${selectedStrategyId}`)
            .then(response => {
                if (!response.ok) throw new Error('加载元数据失败');
                return response.json();
            })
            .then(metadata => {
                displayDbInfo(metadata);
                displayTables(metadata);
            })
            .catch(error => {
                console.error('加载元数据失败:', error);
                alert('无法加载数据库结构，请检查后端服务是否正常');
            });
    }
    function displayDbInfo(metadata) {
        const container = document.getElementById('dbInfo');
        container.innerHTML = `
        <p><strong>IP地址:</strong> ${metadata.url.match(/\/\/([^:\/]+)/)?.[1] || '未知'}</p>
        <p><strong>数据库类型:</strong> ${metadata.databaseProductName || 'MySQL'}</p>
        <p><strong>数据库名称:</strong> ${metadata.catalog || metadata.databaseName || '未知'}</p>
        <p><strong>用户名:</strong> ${metadata.userName || 'root'}</p>
        <p><strong>表数量:</strong> ${metadata.tables ? metadata.tables.length : 0}</p>
    `;
    }
    function displayTables(metadata) {
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
            li.textContent = table.name;
            li.addEventListener('click', () => {
                sqlQueryInput.value = `SELECT * FROM ${table.name};`;
            });
            tableListContainer.appendChild(li);
        });
    }

});
