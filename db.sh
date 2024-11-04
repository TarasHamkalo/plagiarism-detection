docker run -d --rm \
  --name plagdet-db \
  -e POSTGRES_USER=taras \
  -e POSTGRES_PASSWORD=taras \
  -e POSTGRES_DB=plagdet \
  -v pg_data:/var/lib/postgresql/data \
  -p 5432:5432 \
  postgres
#  --net gamestudio-net \

function clear_indexes() {
  rm -rf /home/taras-hamkalo/repositories/plagiarism-detection/SIMPLE_FS_INDEX/
  docker volume rm pg_data
  docker volume create pg_data
  rm -rf /home/taras-hamkalo/repositories/plagiarism-detection/lucene_index/
}
