# Metrics Integration

This is a guide for the integration with the [Metrics plugin](https://plugins.jenkins.io/metrics/).

## Metrics

The integration provides counters for each individual cause and category that you create, as well as each job build. These counters will reset to zero when jenkins is restarted. The format for the metrics created is `jenkins_bfa_category_<category name>` for each category, `jenkins_bfa_cause_<cause name>` for each cause, and `jenkins_bfa_job__<job_name>__number__<job_build_number>__cause__<cause name>` for each job build. The category, cause, and job names will be escaped by the metrics api to replace any spaces with underscores. 

## Exporting

To export the BFA metrics you can use any plugin that integrates with the Metrics plugin.

### Prometheus
The [prometheus plugin](https://plugins.jenkins.io/prometheus/)
To restructure the metrics into the form that prometheus expects you can add the following into your scrape config:

```yaml
metric_relabel_configs:
  - source_labels: [__name__]
    regex: 'jenkins_bfa_category_(.*)'
    target_label: 'category'
  - source_labels: [__name__]
    regex: 'jenkins_bfa_cause_(.*)'
    target_label: 'cause'
  - source_labels: [__name__]
    regex: 'jenkins_bfa_job__(.*)__number__(.*)__cause__(.*)'
    replacement: '$1'
    target_label: 'jenkins_job'
  - source_labels: [__name__]
    regex: 'jenkins_bfa_job__(.*)__number__(.*)__cause__(.*)'
    replacement: '$2'
    target_label: 'build_number'
  - source_labels: [__name__]
    regex: 'jenkins_bfa_job__(.*)__number__(.*)__cause__(.*)'
    replacement: '$3'
    target_label: 'cause'
  - source_labels: [__name__]
    regex: 'jenkins_bfa_(.*)'
    replacement: 'jenkins_bfa'
    target_label: __name__
```

This will provide a metric called `jenkins_bfa` with labels for the category and specific cause.
