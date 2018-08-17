# github comment settings
#github.dismiss_out_of_range_messages

# for PR
if github.pr_title.include?('[WIP]') || github.pr_labels.include?('PR: wip')
  warn('PR is classed as Work in Progress')
end

# Warn when there is a big PR
warn('a large PR') if git.lines_of_code > 500

# detekt
checkstyle_format.report 'build/reports/detekt/detekt-checkstyle.xml'

# AndroidLint
android_lint.severity = "Error"
android_lint.lint(inline_mode: true)
