#!/bin/bash
# =====================================================
#   run.sh — Run the Expense Analyzer
#   Usage: bash run.sh
# =====================================================
echo "🔨 Compiling ExpenseAnalyzer..."
javac -cp "lib/mysql-connector-j-9.7.0.jar" -d bin src/ExpenseAnalyzer.java

if [ $? -eq 0 ]; then
    echo "✅ Compiled! Launching..."
    java -cp "bin:lib/mysql-connector-j-9.7.0.jar" ExpenseAnalyzer
else
    echo "❌ Compilation failed."
fi


# compoile 
# javac -cp "lib/mysql-connector-j-9.7.0.jar" -d bin src/ExpenseAnalyzer.java

# execute 
# java -cp "bin:lib/mysql-connector-j-9.7.0.jar" ExpenseAnalyzer